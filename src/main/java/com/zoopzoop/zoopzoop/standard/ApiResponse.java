package com.zoopzoop.zoopzoop.standard;

public record ApiResponse<T>(
        String resultCode,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("S-1", "Success", data);
    }
}
