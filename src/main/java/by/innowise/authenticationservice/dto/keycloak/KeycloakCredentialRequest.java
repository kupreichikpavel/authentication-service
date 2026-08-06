package by.innowise.authenticationservice.dto.keycloak;

public record KeycloakCredentialRequest(
    String type,
    String value,
    boolean temporary
) {

}
