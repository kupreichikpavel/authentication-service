package by.innowise.authenticationservice.dto.keycloak;

import java.util.List;
import java.util.Map;

public record KeycloakUserCreateRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Map<String, List<String>> attributes
) {
}
