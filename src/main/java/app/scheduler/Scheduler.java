package app.scheduler;

import app.checker.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Scheduler service responsible for running checks at a fixed interval.
 */
@Service
public class Scheduler {

    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * Virtual thread executor for running checkers concurrently.
     */
    private final ExecutorService executor;

    /**
     * List of checkers to be run by the scheduler.
     */
    private final List<Checker> checkers;

    /**
     * Constructs a new Scheduler with the given list of checkers and executor.
     *
     * @param checkers the list of checkers
     * @param executor the executor service for running checkers
     */
    public Scheduler(List<Checker> checkers, ExecutorService executor) {
        this.checkers = checkers;
        this.executor = executor;
    }

    /**
     * Runs checks for all registered checkers at a fixed interval.
     */
    @Scheduled(fixedDelayString = "${checker.interval-ms}")
    public void runChecks() {
        for (Checker checker : checkers) {
            logger.info("Running checker: {}", checker.getName());
            executor.submit(checker);
        }
    }
}
