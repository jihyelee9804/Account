package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 잔액 사용 정책
     * 사용자가 없는 경우, 계좌가 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
     * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 작거나 큰 경우 실패 응답
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber,
                                     Long amount) {
        // 1. 사용자 조회
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        // 2. 계좌 조회
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        // 잔액 사용 정책에 따른 validation 처리
        validateUseBalance(user, account, amount);
        // 잔액 사용 후 account 잔액 수정
        account.useBalance(amount);
        // 잔액 사용 성공 transaction 저장
        return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, amount, account)
        );

    }
    // 잔액 사용 정책에 따른 validation 처리
    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        // 사용자 아이디와 계좌 소유주가 다른 경우
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        // 계좌가 이미 해지 상태인 경우
        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        // 거래금액이 잔액보다 큰 경우
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    // 잔액 사용 실패 트랜잭션 저장
    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        // 계좌 생성
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        // 잔액 사용 실패 transaction 저장
        saveAndGetTransaction(USE, F, amount, account);
    }

    // transaction 저장 - 성공 시, 실패 시 코드 중복으로 메소드화
    private Transaction saveAndGetTransaction(TransactionType transactionType,
            TransactionResultType transactionResultType, Long amount, Account account) {
        // transaction 을 저장하고 Transaction Entity 객체를 반환한다.
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    // 잔액 사용 취소
    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber,
                                        Long amount) {
        // 취소하려는 거래 조회
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));
        // 계좌 조회
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        // 잔액사용취소 메소드를 호출하고 해당 거래, 사용자 계좌, 거래 금액을 인자로 전달한다.
        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(CANCEL, S, amount, account)
        );
    }

    // 잔액 사용취소 유효성 검사
    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        // 거래계좌와 사용취소계좌가 일치하는지 확인
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }
        if(transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    // 잔액사용취소 실패 트랜잭션 저장
    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        // 계좌 생성
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        // 잔액 사용 실패 transaction 저장
        saveAndGetTransaction(CANCEL, F, amount, account);
    }

    // 거래 확인
    @Transactional
    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
