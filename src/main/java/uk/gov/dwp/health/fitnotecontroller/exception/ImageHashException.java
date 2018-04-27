package uk.gov.dwp.health.fitnotecontroller.exception;

public class ImageHashException extends Exception {
    public ImageHashException(String message) {
        super(message);
    }

    public ImageHashException(Throwable cause) {
        super(cause);
    }
}
