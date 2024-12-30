package scoula.coin.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    REAL_ESTATE_NOT_FOUND(HttpStatus.NOT_FOUND, "ESTATE_001", "부동산 정보를 찾을 수 없습니다."),

    // Order related errors
    ORDER_INVALID_PARAMETERS(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 파라미터가 유효하지 않습니다."),
    ORDER_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_002", "주문 실행에 실패했습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_003", "주문 정보를 찾을 수 없습니다."),
    ORDER_INVALID_MARKET(HttpStatus.BAD_REQUEST, "ORDER_004", "유효하지 않은 마켓입니다."),
    ORDER_INVALID_SIDE(HttpStatus.BAD_REQUEST, "ORDER_005", "유효하지 않은 거래 방향입니다."),
    ORDER_INVALID_VOLUME(HttpStatus.BAD_REQUEST, "ORDER_006", "유효하지 않은 거래량입니다."),
    ORDER_INVALID_PRICE(HttpStatus.BAD_REQUEST, "ORDER_007", "유효하지 않은 가격입니다."),
    ORDER_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_008", "응답 오류"),

    // System errors
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "시스템 오류가 발생했습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
