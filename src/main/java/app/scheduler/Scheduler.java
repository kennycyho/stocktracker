package app.scheduler;

import app.checker.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

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

    private static final int MAX_STARTUP_DELAY_MS = 60_000;

    private final int startupDelayMaxMs;

    /**
     * Constructs a new Scheduler with the given list of checkers and executor.
     * Default startup delay is 60 seconds.
     *
     * @param checkers the list of checkers
     * @param executor the executor service for running checkers
     */
    @Autowired
    public Scheduler(List<Checker> checkers, ExecutorService executor) {
        this(checkers, executor, MAX_STARTUP_DELAY_MS);
    }

    /**
     * Constructs a new Scheduler with a configurable startup delay.
     *
     * @param checkers          the list of checkers
     * @param executor          the executor service for running checkers
     * @param startupDelayMaxMs maximum random delay in ms before each checker starts
     */
    Scheduler(List<Checker> checkers, ExecutorService executor, int startupDelayMaxMs) {
        this.checkers = checkers;
        this.executor = executor;
        this.startupDelayMaxMs = startupDelayMaxMs;
    }

    /**
     * Runs checks for all registered checkers at a fixed interval,
     * with a random delay between each checker submission.
     */
    @Scheduled(fixedDelayString = "${checker.interval-ms}")
    public void runChecks() {
        for (int i = 0; i < checkers.size(); i++) {
            Checker checker = checkers.get(i);
            logger.info("Running checker: {}", checker.getName());
            executor.submit(checker);
            if (i < checkers.size() - 1 && startupDelayMaxMs > 0) {
                try {
                    int delay = ThreadLocalRandom.current().nextInt(startupDelayMaxMs + 1);
                    Thread.sleep(delay);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Scheduler sleep interrupted", e);
                    break;
                }
            }
        }
    }
}
