package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class UseBalance { // 잔액 사용 API에 사용되는 요청, 응답 객체
    /**
     * {
     *     "userId":1,
     *     "accountNumber":"1000000000",
     *     "Amount":1000
     * }
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Request {
        @NotNull // null이 아님 유효성검사
        @Min(1) // 최소값 1
        private Long userId;

        @NotBlank // 빈칸없음 유효성검사
        @Size(min = 10, max = 10) // String 길이 최소 10, 최대 10으로 설정
        private String accountNumber;

        @NotNull
        @Min(10) // 최소거래금액 10원
        @Max(1_000_000_000) // 최대거래금액 10억
        private Long amount;
    }

    /**
     * {
     *     "accountNumber":"1234567890",
     *     "transactionResult":"S"
     *     "transactionId":"c2033bb6d82a4250aecf8e27c49b63f6",
     *     "Amount":1000,
     *     "transactionAt":"2024-02-23T23:26:14.671859"
     * }
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String accountNumber;
        private TransactionResultType transactionResult;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactionAt;

        public static Response from(TransactionDto transactionDto) {
            return Response.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionResult(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .transactionAt(transactionDto.getTransactedAt())
                    .build();

        }

    }

}
