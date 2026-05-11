package com.heilous.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class APIResponse<T> {
    private int status;
    private T data;

    public static <T> APIResponse<T> ok(T data) {
        return new APIResponse<>(HttpStatus.OK.value(), data);
    }
}
