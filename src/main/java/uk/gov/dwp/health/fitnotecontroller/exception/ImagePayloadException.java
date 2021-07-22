package uk.gov.dwp.health.fitnotecontroller.exception;

public class ImagePayloadException extends Exception {
  public ImagePayloadException(String message) {
    super(message);
  }

  public ImagePayloadException(Throwable cause) {
    super(cause);
  }
}
