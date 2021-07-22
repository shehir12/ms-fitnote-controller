package uk.gov.dwp.health.fitnotecontroller.exception;

public class DeclarationException extends Exception {

  public DeclarationException(String reason) {
    super(reason);
  }

  public DeclarationException(Exception innerException) {
    super(innerException);
  }
}
