package test.java.framework.manager;

public class SoftAssertionError extends java.lang.AssertionError {

    /**
     * Constructs an AssertionError with its detail message derived
     * from the specified object, which is converted to a string as
     * defined in section 15.18.1.1 of
     * <cite>The Java&trade; Language Specification</cite>.
     * <p>
     * If the specified object is an instance of {@code Throwable}, it
     * becomes the <i>cause</i> of the newly constructed assertion error.
     *
     * @param detailMessage value to be used in constructing detail message
     * @see Throwable#getCause()
     */
    public SoftAssertionError(Object detailMessage) {
        super(detailMessage);
    }
}
