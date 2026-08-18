package by.innowise.authenticationservice.dto.keycloak;

import java.util.List;
import java.util.Map;

public record KeycloakUserCreateRequest(
    String username,
    String email,
    boolean enabled,
    Map<String, List<String>> attributes
) {

}
