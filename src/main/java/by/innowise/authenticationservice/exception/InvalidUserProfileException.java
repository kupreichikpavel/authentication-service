package by.innowise.authenticationservice.exception;

public class InvalidUserProfileException extends RuntimeException {

    public InvalidUserProfileException() {
        super("User profile data was rejected");
    }
}
