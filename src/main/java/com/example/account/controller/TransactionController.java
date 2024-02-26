package com.example.account.controller;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    @PostMapping("/transaction/use")
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request
    ){

        try {
            return UseBalance.Response.from(
                transactionService.useBalance(request.getUserId(),
                    request.getAccountNumber(), request.getAmount()));
        } catch (AccountException e) {
            log.error("Failed to use balance. ");

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    // 잔액 사용 취소
    @PostMapping("/transaction/cancel")
    public CancelBalance.Response useBalance(
            @Valid @RequestBody CancelBalance.Request request
    ){

        try {
            // 잔액사용취소 처리를 시도한다.
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount()));
        } catch (AccountException e) {
            // 잔액사용취소 처리가 실패 시, 로그를 남기고 실패 트랜잭션을 저장한다.
            log.error("Failed to use balance. ");

            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    // 거래 확인
    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId
    ) {
        return QueryTransactionResponse.from(transactionService
                .queryTransaction(transactionId));
    }
}
