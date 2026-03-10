package io.attestry.commonlib.infrastructure;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }


    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }


    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }


    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message, "ERROR"));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
