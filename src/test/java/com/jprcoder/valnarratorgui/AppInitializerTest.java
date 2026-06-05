package com.jprcoder.valnarratorgui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class AppInitializerTest {

    @AfterEach
    void clearInterruptFlag() {
        assertFalse(Thread.interrupted() && Thread.currentThread().isInterrupted());
    }

    @Test
    void sleepMsReturnsFalseAndRestoresInterruptFlagWhenSleeperThrows() {
        boolean result = AppInitializer.sleepMs(42, ms -> {
            throw new InterruptedException("simulated interrupt");
        });

        assertFalse(result);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void confirmValorantClosedReturnsTrueWhenAllChecksStayAbsent() {
        AtomicInteger sleepCalls = new AtomicInteger();
        BooleanSupplier isValorantRunning = () -> false;

        boolean result = AppInitializer.confirmValorantClosed(
                isValorantRunning,
                ms -> sleepCalls.incrementAndGet(),
                3,
                1_500L);

        assertTrue(result);
        assertEquals(3, sleepCalls.get());
    }

    @Test
    void confirmValorantClosedFailsWhenValorantRecoversDuringTheConfirmationWindow() {
        AtomicInteger runningChecks = new AtomicInteger();
        BooleanSupplier isValorantRunning = () -> runningChecks.getAndIncrement() == 1;

        boolean result = AppInitializer.confirmValorantClosed(
                isValorantRunning,
                ms -> {
                },
                3,
                1_500L);

        assertFalse(result);
        assertEquals(2, runningChecks.get());
    }

    @Test
    void confirmValorantClosedStopsWhenSleeperIsInterrupted() {
        AtomicInteger runningChecks = new AtomicInteger();
        BooleanSupplier isValorantRunning = () -> {
            runningChecks.incrementAndGet();
            return false;
        };

        boolean result = AppInitializer.confirmValorantClosed(
                isValorantRunning,
                ms -> {
                    throw new InterruptedException("stop");
                },
                3,
                1_500L);

        assertFalse(result);
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(0, runningChecks.get());
    }
}

