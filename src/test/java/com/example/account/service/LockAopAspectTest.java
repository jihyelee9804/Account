package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.*;
class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

//    @Test
//    void lockAndUnlock() throws Throwable {
//        //given
//        ArgumentCaptor<String> lockArgumentCaptor =
//                ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> unlockArgumentCaptor =
//                ArgumentCaptor.forClass(String.class);
//        UseBalance.Request request = new UseBalance.Request(123L, "1234", 1000L);
//        //when
//        lockAopAspect.aroundMethod(proceedingJoinPoint, request);
//        //then
//        verify(lockService, times(1).lock(lockArgumentCaptor.capture()));
//        verify(lockService, times(1).unlock(unlockArgumentCaptor.capture()));
//        assertEquals("1234", lockArgumentCaptor.getValue());
//        assertEquals("1234", unlockArgumentCaptor.getValue());
//    }
//
//    @Test
//    void lockAndUnlock_evenIfThrow() throws Throwable {
//        //given
//        ArgumentCaptor<String> lockArgumentCaptor =
//                ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> unlockArgumentCaptor =
//                ArgumentCaptor.forClass(String.class);
//        UseBalance.Request request = new UseBalance.Request(123L, "54321", 1000L);
//        given(proceedingJoinPoint.proceed())
//                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
//
//        //when
//        assertThrows(AccountException.class, () ->
//        lockAopAspect.aroundMethod(proceedingJoinPoint, request));
//        //then
//        verify(lockService, times(1).lock(lockArgumentCaptor.capture()));
//        verify(lockService, times(1).unlock(unlockArgumentCaptor.capture()));
//        assertEquals("54321", lockArgumentCaptor.getValue());
//        assertEquals("54321", unlockArgumentCaptor.getValue());
//    }
}