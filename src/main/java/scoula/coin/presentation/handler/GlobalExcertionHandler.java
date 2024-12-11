package scoula.coin.presentation.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import scoula.coin.application.dto.ErrorResponse;
import scoula.coin.global.error.CustomException;
import scoula.coin.global.error.ErrorCode;

@RestControllerAdvice
public class GlobalExcertionHandler {

    //@ExceptionHandler(CustomException.class)
    //public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
    //    ErrorCode errorCode = ex.getErrorCode();
    //    ErrorResponse response = new ErrorResponse(
    //            errorCode.getStatus(),
    //            errorCode.getCode(),
    //            errorCode.getMessage()
    //    );
    //    return ResponseEntity.status(errorCode.getStatus()).body(response);
    //}

    //@ExceptionHandler(Exception.class)
    //public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    //    ErrorResponse response = new ErrorResponse(
    //            HttpStatus.INTERNAL_SERVER_ERROR,
    //            "UNKNOWN_ERROR",
    //            "예기치 못한 오류가 발생했습니다."
    //    );
    //    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    //}
}
