package com.example.account.exception;

import com.example.account.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.account.type.ErrorCode.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice // 전역에서 발생하는 exception을 처리하겠다.
public class GlobalExceptionHandler {
    // exceptiona이 발생하면 클라이언트로 예외를 던지지 않고 ErrorResponse로 응답해 예외처리한다.
    // 1. AccountException이 발생한 경우
    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is occured.", e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }
    // 이외에 자주 발생하는 특정 예외를 별도로 정의해도 된다.

    // 2. Exception이 발생한 경우
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGlobalException(Exception e) {
        log.error("Exception is occured.", e);

        return new ErrorResponse(
                INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR.getDescription());
    }
}
