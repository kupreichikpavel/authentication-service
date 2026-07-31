package by.innowise.authenticationservice.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakRoleResponse(
        String id,
        String name
) {
}
