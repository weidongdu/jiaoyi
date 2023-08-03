package pro.jiaoyi.common.model;

import lombok.Data;

@Data
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    private ApiResult() {
    }

    private ApiResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private ApiResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "success");
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> success(String msg, T data) {
        return new ApiResult<>(200, msg, data);
    }

    public static <T> ApiResult<T> success(int code, String msg, T data) {
        return new ApiResult<>(code, msg, data);
    }

    public static <T> ApiResult<T> error() {
        return new ApiResult<>(500, "error");
    }

    public static <T> ApiResult<T> error(String msg) {
        return new ApiResult<>(500, msg);
    }

    public static <T> ApiResult<T> error(int code, String msg) {
        return new ApiResult<>(code, msg);
    }


}
