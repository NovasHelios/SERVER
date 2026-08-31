package com.heilous.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode {

    // 공통/인증 에러
    INVALID_CREDENTIALS(401, "AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCESS_DENIED(403, "AUTH_002", "해당 작업을 수행할 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(409, "AUTH_003", "이미 가입된 이메일입니다."),
    EMAIL_VERIFICATION_FAILED(400, "AUTH_004", "이메일 인증 코드가 일치하지 않습니다."),
    INVALID_INPUT(400, "AUTH_005", "잘못된 입력입니다."),

    // 사용자/사업자 에러
    USER_NOT_FOUND(404, "USER_001", "사용자를 찾을 수 없습니다."),
    BUSINESS_NUMBER_ALREADY_EXISTS(409, "COMP_001", "이미 등록된 사업자 번호입니다."),
    LAND_NOT_FOUND(404, "LAND_001", "토지를 찾을 수 없습니다."),
    LAND_ADDRESS_ALREADY_EXISTS(409, "LAND_002", "이미 등록된 토지 주소입니다."),

    // 찜하기 에러
    WISH_NOT_FOUND(404, "WISH_001", "찜한 토지를 찾을 수 없습니다."),
    WISH_ALREADY_EXISTS(409, "WISH_002", "이미 찜한 토지입니다."),

    // 채팅 에러
    CHAT_ROOM_NOT_FOUND(404, "CHAT_001", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(403, "CHAT_002", "해당 채팅방에 접근할 권한이 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "CHAT_003", "이미 해당 토지의 상담 채팅방이 있습니다."),
    CHAT_ROOM_NOT_ACTIVE(409, "CHAT_004", "현재 상태에서는 채팅을 진행할 수 없습니다."),

    // 비밀번호 찾기 에러
    PASSWORD_RESET_CODE_INVALID(400, "AUTH_006", "인증 코드가 올바르지 않거나 만료되었습니다."),
    PASSWORD_RESET_NOT_VERIFIED(400, "AUTH_007", "이메일 인증이 완료되지 않았습니다."),
    PASSWORD_MISMATCH(400, "AUTH_008", "비밀번호가 일치하지 않습니다."),

    // 외부 API 에러
    EXTERNAL_API_ERROR(502, "EXT_001", "외부 API 호출 중 오류가 발생했습니다."),
    KAKAO_ADDRESS_NOT_FOUND(404, "EXT_002", "해당 주소에 대한 검색 결과를 찾을 수 없습니다."),

    // 파일 업로드 에러
    INVALID_IMAGE_TYPE(400, "FILE_001", "허용되지 않는 이미지 형식입니다. (jpg, png, webp, gif만 허용)"),
    INVALID_DOCUMENT_TYPE(400, "FILE_003", "허용되지 않는 문서 형식입니다. (pdf, jpg, png만 허용)"),
    FILE_UPLOAD_FAILED(500, "FILE_002", "파일 저장 중 오류가 발생했습니다."),
    LAND_IMAGE_REQUIRED(400, "FILE_004", "토지 이미지는 최소 3장 이상 등록해야 합니다."),
    LAND_IMAGE_MAX_EXCEEDED(400, "FILE_005", "토지 이미지는 최대 5장까지 등록할 수 있습니다."),
    LAND_IMAGE_NOT_FOUND(404, "FILE_006", "해당 토지 이미지를 찾을 수 없습니다."),

    // 데이터 무결성 에러
    DATA_INTEGRITY_VIOLATION(409, "DATA_001", "데이터 무결성 오류가 발생했습니다. 연관된 데이터를 확인해주세요.");

    private final int status;
    private final String code;
    private final String message;
}
