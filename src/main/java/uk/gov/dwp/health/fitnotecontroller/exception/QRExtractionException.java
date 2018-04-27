package uk.gov.dwp.health.fitnotecontroller.exception;

public class QRExtractionException extends Exception {
    public QRExtractionException(String message, Throwable e) {
        super(message, e);
    }

    public QRExtractionException(String message) {
        super(message);
    }
}
