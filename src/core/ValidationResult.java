package core;

/** Outcome of validating a chain: valid, or which block failed and why. */
public class ValidationResult {
    private final boolean valid;
    private final int invalidBlockIndex;
    private final String reason;

    private ValidationResult(boolean valid, int invalidBlockIndex, String reason) {
        this.valid = valid;
        this.invalidBlockIndex = invalidBlockIndex;
        this.reason = reason;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, -1, "All checks passed.");
    }

    public static ValidationResult fail(int index, String reason) {
        return new ValidationResult(false, index, reason);
    }

    public boolean isValid() { return valid; }
    public int getInvalidBlockIndex() { return invalidBlockIndex; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return valid ? "VALID - " + reason
                     : "INVALID at block #" + invalidBlockIndex + " - " + reason;
    }
}
