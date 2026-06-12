package org.example.scheduler;

import org.example.checker.StockChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private final List<StockChecker> checkers;

    public Scheduler(List<StockChecker> checkers) {
        this.checkers = checkers;
    }

    @Scheduled(fixedDelayString = "${checker.interval-ms}")
    public void runChecks() {
        for (StockChecker checker : checkers) {
            LOGGER.info("Running checker: {}", checker.getClass().getSimpleName());
            checker.check();
        }
    }
}
