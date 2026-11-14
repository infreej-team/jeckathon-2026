package dev.infreej.jeckathon.global.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// errors는 입력값 검증 실패에만 포함한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, List<FieldError> errors) {

	public record FieldError(String field, String reason) {
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, null);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
	}
}
