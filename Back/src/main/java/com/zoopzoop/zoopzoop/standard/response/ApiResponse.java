package com.zoopzoop.zoopzoop.standard.response;

public record ApiResponse<T>(
        String resultCode,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("S-1", "Success", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("F-1", message, null);
    }
}
