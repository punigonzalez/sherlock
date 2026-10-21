package com.sherlock.logdetective.ingestion.adapter.out.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.domain.SourceHealth.HealthStatus;
import com.sherlock.logdetective.ingestion.domain.SourceType;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

/**
 * Local file tail source with append/truncate/rotation/partial-line/multiline handling.
 */
public final class FileLogSource implements LogSource {

	private static final Pattern CONTINUATION = Pattern.compile(
			"^(?:\\s+at\\s|\\s+\\.\\.\\.\\s|\\s*Caused by:|Traceback \\(most recent call last\\):|\\s+File \"|[\\w.$]+(?:Exception|Error)(?::.*)?).*"
	);

	private final LogSourceId id;
	private final Path path;
	private final Charset charset;
	private final AtomicReference<SourceHealth> health = new AtomicReference<>(
			SourceHealth.of(HealthStatus.NOT_STARTED, "not started")
	);
	private volatile boolean running;

	public FileLogSource(LogSourceId id, Path path) {
		this(id, path, StandardCharsets.UTF_8);
	}

	public FileLogSource(LogSourceId id, Path path, Charset charset) {
		this.id = Objects.requireNonNull(id, "id");
		this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
		this.charset = Objects.requireNonNull(charset, "charset");
	}

	public Path path() {
		return path;
	}

	@Override
	public LogSourceId id() {
		return id;
	}

	@Override
	public SourceType type() {
		return SourceType.FILE;
	}

	@Override
	public void validate() {
		Path parent = path.getParent();
		if (parent != null && Files.exists(parent) && !Files.isDirectory(parent)) {
			throw new IllegalStateException("Parent path is not a directory: " + parent);
		}
		if (Files.exists(path) && !Files.isRegularFile(path)) {
			throw new IllegalStateException("Log path is not a regular file: " + path);
		}
		if (Files.exists(path) && !Files.isReadable(path)) {
			health.set(SourceHealth.of(HealthStatus.PERMISSION_DENIED, "not readable: " + path));
			throw new IllegalStateException("Log file is not readable: " + path);
		}
	}

	@Override
	public void start() {
		validate();
		running = true;
		refreshHealth();
	}

	@Override
	public void stop() {
		running = false;
		health.set(SourceHealth.of(HealthStatus.STOPPED, "stopped"));
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public SourceHealth health() {
		if (running) {
			refreshHealth();
		}
		return health.get();
	}

	@Override
	public List<RawLogRecord> fetch(FetchCursor cursor, int limit) {
		if (!running) {
			return List.of();
		}
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be >= 1");
		}
		if (!Files.exists(path)) {
			health.set(SourceHealth.of(HealthStatus.UNAVAILABLE, "file missing: " + path));
			return List.of();
		}
		if (!Files.isReadable(path)) {
			health.set(SourceHealth.of(HealthStatus.PERMISSION_DENIED, "not readable: " + path));
			return List.of();
		}

		try {
			FileCursor state = FileCursor.decode(cursor);
			long inode = readInode();
			long size = Files.size(path);

			if (state.inode() != 0 && state.inode() != inode) {
				// Rotation / replacement
				state = FileCursor.reset(inode, state.partial());
				health.set(SourceHealth.of(HealthStatus.DEGRADED, "rotation detected; rewound"));
			}
			else if (size < state.offset()) {
				// Truncation
				state = FileCursor.reset(inode, "");
				health.set(SourceHealth.of(HealthStatus.DEGRADED, "truncation detected; rewound"));
			}
			else if (state.inode() == 0) {
				state = new FileCursor(inode, state.offset(), state.partial());
			}

			byte[] chunk;
			long newOffset;
			try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
				raf.seek(state.offset());
				int toRead = (int) Math.min(Integer.MAX_VALUE, size - state.offset());
				chunk = new byte[toRead];
				int read = raf.read(chunk);
				if (read < 0) {
					read = 0;
				}
				if (read < chunk.length) {
					byte[] slim = new byte[read];
					System.arraycopy(chunk, 0, slim, 0, read);
					chunk = slim;
				}
				newOffset = state.offset() + chunk.length;
			}

			String text = state.partial() + new String(chunk, charset);
			List<String> completeLines = new ArrayList<>();
			String partial = "";
			int searchFrom = 0;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '\n') {
					String line = text.substring(searchFrom, i);
					if (line.endsWith("\r")) {
						line = line.substring(0, line.length() - 1);
					}
					completeLines.add(line);
					searchFrom = i + 1;
				}
			}
			if (searchFrom < text.length()) {
				partial = text.substring(searchFrom);
			}

			List<AssembledLine> assembled = assembleMultiline(completeLines, state.offset());
			List<RawLogRecord> records = new ArrayList<>();
			for (AssembledLine line : assembled) {
				if (records.size() >= limit) {
					break;
				}
				Map<String, String> headers = new LinkedHashMap<>();
				headers.put("source.path", path.toString());
				headers.put("source.byteStart", Long.toString(line.byteStart()));
				headers.put("source.byteEnd", Long.toString(line.byteEnd()));
				headers.put("source.inode", Long.toString(inode));
				String payloadId = id.value() + ":" + inode + ":" + line.byteStart() + ":" + line.byteEnd();
				records.add(new RawLogRecord(payloadId, Instant.now(), line.body(), Map.copyOf(headers)));
			}

			// Encode next cursor including unconsumed assembled lines + partial.
			// For simplicity: if we hit limit mid-batch, rewind offset to first unconsumed line start.
			long checkpointOffset = newOffset;
			String checkpointPartial = partial;
			if (assembled.size() > records.size()) {
				AssembledLine firstUnconsumed = assembled.get(records.size());
				checkpointOffset = firstUnconsumed.byteStart();
				checkpointPartial = "";
			}
			else if (!assembled.isEmpty() && records.size() == assembled.size()) {
				checkpointOffset = newOffset;
				checkpointPartial = partial;
			}

			FileCursor next = new FileCursor(inode, checkpointOffset, checkpointPartial);
			// Stash next cursor opaque into the last record header for advance(); also keep on thread-local-like field.
			lastComputedCursor = next;
			health.set(SourceHealth.of(HealthStatus.HEALTHY, "ok offset=" + checkpointOffset));
			return List.copyOf(records);
		}
		catch (IOException ex) {
			health.set(SourceHealth.of(HealthStatus.UNAVAILABLE, ex.getMessage()));
			return List.of();
		}
	}

	private volatile FileCursor lastComputedCursor = FileCursor.start();

	@Override
	public FetchCursor advance(FetchCursor cursor, List<RawLogRecord> consumed) {
		if (consumed == null || consumed.isEmpty()) {
			return lastComputedCursor == null ? cursor : lastComputedCursor.encode();
		}
		// Prefer cursor derived from last fetch.
		if (lastComputedCursor != null) {
			return lastComputedCursor.encode();
		}
		RawLogRecord last = consumed.get(consumed.size() - 1);
		long end = Long.parseLong(last.headers().getOrDefault("source.byteEnd", "0"));
		long inode = Long.parseLong(last.headers().getOrDefault("source.inode", "0"));
		return new FileCursor(inode, end, "").encode();
	}

	private void refreshHealth() {
		if (!Files.exists(path)) {
			health.set(SourceHealth.of(HealthStatus.UNAVAILABLE, "file missing: " + path));
		}
		else if (!Files.isReadable(path)) {
			health.set(SourceHealth.of(HealthStatus.PERMISSION_DENIED, "not readable: " + path));
		}
		else {
			health.set(SourceHealth.of(HealthStatus.HEALTHY, "readable"));
		}
	}

	private long readInode() throws IOException {
		try {
			Object value = Files.getAttribute(path, "unix:ino");
			if (value instanceof Number number) {
				return number.longValue();
			}
		}
		catch (UnsupportedOperationException ignored) {
			// fall through
		}
		// Fallback identity: file key from abs path hash + size snapshot is weaker; use path hash bits.
		return path.toString().hashCode() & 0xffffffffL;
	}

	private List<AssembledLine> assembleMultiline(List<String> lines, long startingOffsetHint) {
		List<AssembledLine> assembled = new ArrayList<>();
		StringBuilder current = null;
		long start = startingOffsetHint;
		long end = startingOffsetHint;
		// We cannot perfectly reconstruct byte offsets after charset decode for variable-width encodings.
		// Use cumulative UTF-8 byte lengths of emitted records relative to read window start as stable ids.
		long cursor = startingOffsetHint;
		for (String line : lines) {
			byte[] lineBytes = (line + "\n").getBytes(charset);
			long lineStart = cursor;
			long lineEnd = cursor + lineBytes.length;
			cursor = lineEnd;
			if (current == null) {
				current = new StringBuilder(line);
				start = lineStart;
				end = lineEnd;
				continue;
			}
			if (isContinuation(line)) {
				current.append('\n').append(line);
				end = lineEnd;
			}
			else {
				assembled.add(new AssembledLine(start, end, current.toString()));
				current = new StringBuilder(line);
				start = lineStart;
				end = lineEnd;
			}
		}
		if (current != null) {
			assembled.add(new AssembledLine(start, end, current.toString()));
		}
		return assembled;
	}

	static boolean isContinuation(String line) {
		return CONTINUATION.matcher(line).matches();
	}

	private record AssembledLine(long byteStart, long byteEnd, String body) {
	}

	record FileCursor(long inode, long offset, String partial) {

		static FileCursor start() {
			return new FileCursor(0L, 0L, "");
		}

		static FileCursor reset(long inode, String partial) {
			return new FileCursor(inode, 0L, partial == null ? "" : partial);
		}

		static FileCursor decode(FetchCursor cursor) {
			if (cursor == null || cursor.isStart()) {
				return start();
			}
			String[] parts = cursor.opaque().split(":", 3);
			if (parts.length < 2) {
				return start();
			}
			long inode = Long.parseLong(parts[0]);
			long offset = Long.parseLong(parts[1]);
			String partial = parts.length == 3 ? new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8) : "";
			return new FileCursor(inode, offset, partial);
		}

		FetchCursor encode() {
			String encodedPartial = Base64.getUrlEncoder().withoutPadding()
					.encodeToString((partial == null ? "" : partial).getBytes(StandardCharsets.UTF_8));
			return new FetchCursor(inode + ":" + offset + ":" + encodedPartial);
		}
	}
}
