package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_checkpoints")
public class SourceCheckpointEntity {

	@Id
	@Column(name = "source_id")
	private String sourceId;

	@Column(name = "cursor_opaque", nullable = false)
	private String cursorOpaque;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SourceCheckpointEntity() {
	}

	public SourceCheckpointEntity(String sourceId, String cursorOpaque, Instant updatedAt) {
		this.sourceId = sourceId;
		this.cursorOpaque = cursorOpaque;
		this.updatedAt = updatedAt;
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getCursorOpaque() {
		return cursorOpaque;
	}

	public void setCursorOpaque(String cursorOpaque) {
		this.cursorOpaque = cursorOpaque;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
