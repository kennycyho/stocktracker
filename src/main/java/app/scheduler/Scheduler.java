package app.scheduler;

import app.checker.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduler service responsible for running checks at a fixed interval.
 */
@Service
public class Scheduler {

    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    
    /**
     * List of checkers to be run by the scheduler.
     */
    private final List<Checker> checkers;

    /**
     * Constructs a new Scheduler with the given list of checkers.
     *
     * @param checkers the list of checkers
     */
    public Scheduler(List<Checker> checkers) {
        this.checkers = checkers;
    }

    /**
     * Runs checks for all registered checkers at a fixed interval.
     */
    @Scheduled(fixedDelayString = "${checker.interval-ms}")
    public void runChecks() {
        for (Checker checker : checkers) {
            logger.info("Running checker: {}", checker.getName());
            try {
                checker.check();
            }
            catch (Exception e) {
                logger.error("Checker {} threw an unexpected exception", checker.getName(), e);
            }
        }
    }
}
