package by.innowise.authenticationservice.exception;

public class IdentityProviderException extends RuntimeException {

  public IdentityProviderException(String message) {
    super(message);
  }

  public IdentityProviderException(
      String message,
      Throwable cause
  ) {
    super(message, cause);
  }
}
