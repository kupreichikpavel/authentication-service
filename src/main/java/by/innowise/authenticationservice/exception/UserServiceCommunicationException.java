package by.innowise.authenticationservice.exception;

public class UserServiceCommunicationException extends RuntimeException {

  public UserServiceCommunicationException(String message) {
    super(message);
  }

  public UserServiceCommunicationException(
      String message,
      Throwable cause
  ) {
    super(message, cause);
  }
}
