package ddia.example.ddia.chaos;

/**
 * Interface for chaos engineering experiments.
 *
 * This interface defines the contract for different types of chaos experiments
 * that can be executed to test system resilience.
 *
 * Chaos engineering principles:
 * 1. Build a hypothesis around steady state behavior
 * 2. Vary real-world events
 * 3. Run experiments in production
 * 4. Automate experiments to run continuously
 * 5. Minimize blast radius
 */
public interface ChaosExperiment {

    /**
     * Gets the name of the chaos experiment.
     *
     * @return Human-readable name of the experiment
     */
    String getName();

    /**
     * Gets a description of what this experiment tests.
     *
     * @return Description of the experiment's purpose
     */
    String getDescription();

    /**
     * Executes the chaos experiment.
     *
     * Implementations should:
     * - Log the start and end of the experiment
     * - Handle exceptions gracefully
     * - Measure the impact of the experiment
     * - Clean up any resources used
     */
    void execute();

    /**
     * Gets the expected duration of the experiment in milliseconds.
     *
     * @return Duration in milliseconds
     */
    default long getExpectedDurationMs() {
        return 5000; // 5 seconds default
    }

    /**
     * Gets the severity level of this experiment.
     *
     * @return Severity level (LOW, MEDIUM, HIGH)
     */
    default ExperimentSeverity getSeverity() {
        return ExperimentSeverity.MEDIUM;
    }

    /**
     * Checks if this experiment is currently enabled.
     *
     * @return true if the experiment should run, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Severity levels for chaos experiments.
     */
    enum ExperimentSeverity {
        LOW,    // Minimal impact, safe to run frequently
        MEDIUM, // Moderate impact, run with caution
        HIGH    // High impact, run sparingly and with monitoring
    }
}