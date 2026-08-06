package by.innowise.authenticationservice.handler;

import by.innowise.authenticationservice.exception.IdentityProviderException;
import by.innowise.authenticationservice.exception.InvalidAuthenticationDataException;
import by.innowise.authenticationservice.exception.InvalidUserProfileException;
import by.innowise.authenticationservice.exception.UserAlreadyExistsException;
import by.innowise.authenticationservice.exception.UserServiceCommunicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidAuthenticationDataException.class)
  public ResponseEntity<ProblemDetail> handleInvalidAuthenticationData(
      InvalidAuthenticationDataException exception
  ) {
    return createResponse(
        HttpStatus.UNAUTHORIZED,
        "Authentication failed",
        exception.getMessage()
    );
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleUserAlreadyExists(
      UserAlreadyExistsException exception
  ) {
    return createResponse(
        HttpStatus.CONFLICT,
        "Registration conflict",
        exception.getMessage()
    );
  }

  @ExceptionHandler(InvalidUserProfileException.class)
  public ResponseEntity<ProblemDetail> handleInvalidUserProfile(
      InvalidUserProfileException exception
  ) {
    return createResponse(
        HttpStatus.BAD_REQUEST,
        "Invalid user profile",
        exception.getMessage()
    );
  }

  @ExceptionHandler(IdentityProviderException.class)
  public ResponseEntity<ProblemDetail> handleIdentityProviderException(
      IdentityProviderException exception
  ) {
    log.error(
        "Identity provider communication failed",
        exception
    );

    return createResponse(
        HttpStatus.BAD_GATEWAY,
        "Keycloak communication error",
        "Identity provider is unavailable"
    );
  }

  @ExceptionHandler(UserServiceCommunicationException.class)
  public ResponseEntity<ProblemDetail> handleUserServiceCommunication(
      UserServiceCommunicationException exception
  ) {
    log.error(
        "Failed to request data from User Service",
        exception
    );

    return createResponse(
        HttpStatus.BAD_GATEWAY,
        "User Service communication error",
        "User Service is unavailable"
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException exception
  ) {
    Map<String, String> errors = new LinkedHashMap<>();

    exception.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(
            error.getField(),
            error.getDefaultMessage()
        ));

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Request validation failed"
    );

    problem.setTitle("Invalid request");
    problem.setProperty("errors", errors);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpectedException(
      Exception exception
  ) {
    log.error("Unexpected application error", exception);

    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "An unexpected error occurred"
    );
  }

  private ResponseEntity<ProblemDetail> createResponse(
      HttpStatus status,
      String title,
      String detail
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        status,
        detail
    );

    problem.setTitle(title);

    return ResponseEntity
        .status(status)
        .body(problem);
  }
}
