package app.scheduler;

import app.checker.Checker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SchedulerTest {

    private Checker checker1;
    private Checker checker2;
    private ExecutorService executor;
    private Scheduler scheduler;

    @BeforeEach
    void setUp() {
        checker1 = mock(Checker.class);
        checker2 = mock(Checker.class);
        when(checker1.getName()).thenReturn("Checker1");
        when(checker2.getName()).thenReturn("Checker2");

        executor = mock(ExecutorService.class);
        scheduler = new Scheduler(List.of(checker1, checker2), executor, 0);
    }

    @Test
    void runChecks_submitsEachCheckerToExecutor() {
        scheduler.runChecks();

        verify(executor, times(1)).submit(checker1);
        verify(executor, times(1)).submit(checker2);
    }

    @Test
    void runChecks_withEmptyCheckerList_doesNotSubmitAnything() {
        Scheduler emptyScheduler = new Scheduler(List.of(), executor, 0);

        emptyScheduler.runChecks();

        verifyNoInteractions(executor);
    }

    @Test
    void runChecks_submitsCheckersInRegistrationOrder() {
        scheduler.runChecks();

        InOrder inOrder = inOrder(executor);
        inOrder.verify(executor).submit(checker1);
        inOrder.verify(executor).submit(checker2);
    }

    @Test
    void runChecks_continuesSubmittingRemainingCheckersEvenIfOneThrows() {
        Checker throwingChecker = mock(Checker.class);
        when(throwingChecker.getName()).thenReturn("ThrowingChecker");

        Scheduler mixedScheduler = new Scheduler(
                List.of(throwingChecker, checker1), executor, 0);

        assertDoesNotThrow(mixedScheduler::runChecks);

        verify(executor).submit(throwingChecker);
        verify(executor).submit(checker1);
    }
}