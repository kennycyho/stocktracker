package app.scheduler;

import app.checker.Checker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Test
    void runChecks_callsCheckOnAllCheckers_whenScheduled() {
        Checker checker1 = mock(Checker.class);
        Checker checker2 = mock(Checker.class);
        List<Checker> checkers = List.of(checker1, checker2);
        
        Scheduler scheduler = new Scheduler(checkers);

        scheduler.runChecks();

        verify(checker1).check();
        verify(checker2).check();
    }
}
