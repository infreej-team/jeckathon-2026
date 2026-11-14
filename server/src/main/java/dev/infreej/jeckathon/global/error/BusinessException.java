package dev.infreej.jeckathon.global.error;

import lombok.Getter;

// 서비스 로직에서 의도한 실패를 알릴 때 던진다. 예: throw new BusinessException(ErrorCode.NOT_FOUND);
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
