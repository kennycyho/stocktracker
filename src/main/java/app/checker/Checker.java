package app.checker;

/**
 * Interface for a checker that performs checks on products.
 */
public interface Checker {

    /**
     * Performs the check operation.
     */
    void check();

    /**
     * Returns the name of the checker.
     *
     * @return the name of the checker
     */
    String getName();
}
