package scoula.coin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.of(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.of(false, null, message);
    }
}
