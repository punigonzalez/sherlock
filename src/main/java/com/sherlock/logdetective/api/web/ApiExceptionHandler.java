package com.sherlock.logdetective.api.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import com.sherlock.logdetective.api.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.sherlock.logdetective.api")
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, ServletWebRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(ApiExceptionHandler::formatFieldError)
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequest(), details);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, ServletWebRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequest(), List.of());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> conflict(IllegalStateException ex, ServletWebRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequest(), List.of());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> status(ResponseStatusException ex, ServletWebRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
		return build(status, message, request.getRequest(), List.of());
	}

	private static ResponseEntity<ApiError> build(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			List<String> details
	) {
		ApiError body = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message == null ? status.getReasonPhrase() : message,
				request.getRequestURI(),
				details
		);
		return ResponseEntity.status(status).body(body);
	}

	private static String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
