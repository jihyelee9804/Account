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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.*;
import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 1000L;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountUserRepository accountUserRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L,
                "1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());

    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void useBalanceFail_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void useBalanceFail_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 다름")
    void useBalanceFail_userUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해지 계좌")
    void useBalanceFail_unregisteredAccount() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        // 사용자 정상 조회됨
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        // 사용자 계좌가 unregistered 상태임
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 잔액 초과 거래 금액")
    void useBalanceFail_exceedAmount() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        // 잔액이 100원 있는 계좌
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();
        // 사용자 정상 조회
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        // 계좌 정상 조회
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        // 잔액 100원 계좌에서 1000원 사용 - 에러 발생
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        // 에러 기대값과 실제값 비교
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        // transaction 테이블에 레코드 저장 안됨
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("잔액 사용 실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        // 거래실패저장
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        // 거래금액 기대값:200, 실제값:200
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        // 거래후계좌잔액 기대값:10000, 실제값:10000
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        // 거래결과 기대값:F, 실제값:F
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        // 계좌 생성
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        // 잔액 사용 거래 생성
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        // 거래 반환
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        // 계좌 반환
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        // 잔액 사용 취소 거래 저장
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelBalanceFail_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        // 에러코드가 일치하는지 확인
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 원 사용 거래 없음")
    void cancelBalanceFail_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        // 에러코드가 일치하는지 확인
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelBalanceFail_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        // 계좌 생성
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        Account accountNotUSe = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        // 잔액 사용 거래 생성
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUSe));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        // 에러코드가 일치하는지 확인
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }
}