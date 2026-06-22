package app.scheduler;

import app.checker.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private final List<Checker> checkers;

    public Scheduler(List<Checker> checkers) {
        this.checkers = checkers;
    }

    @Scheduled(fixedDelayString = "${checker.interval-ms}")
    public void runChecks() {
        for (Checker checker : checkers) {
            LOGGER.info("Running checker: {}", checker.getName());
            checker.check();
        }
    }
}
