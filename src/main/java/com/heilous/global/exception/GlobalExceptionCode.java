package com.heilous.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalExceptionCode {

    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 오류"),

    INVALID_INPUT(400, "INVALID_INPUT", "잘못된 요청입니다.");

    private final int status;
    private final String code;
    private final String message;
}