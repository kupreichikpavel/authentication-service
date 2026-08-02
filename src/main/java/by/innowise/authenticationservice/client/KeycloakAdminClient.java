package by.innowise.authenticationservice.client;

import by.innowise.authenticationservice.config.KeycloakProperties;
import by.innowise.authenticationservice.dto.keycloak.KeycloakCredentialRequest;
import by.innowise.authenticationservice.dto.keycloak.KeycloakRoleResponse;
import by.innowise.authenticationservice.dto.keycloak.KeycloakUserCreateRequest;
import by.innowise.authenticationservice.exception.IdentityProviderException;
import by.innowise.authenticationservice.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

  private static final String DEFAULT_ROLE = "USER";

  private final RestClient restClient;
  private final KeycloakProperties properties;
  private final KeycloakTokenClient tokenClient;

  public String createUser(KeycloakUserCreateRequest request, String password) {
    String accessToken = tokenClient.createServiceAccessToken();

    String keycloakUserId = createUserRecord(accessToken, request);

    try {
      setPassword(accessToken, keycloakUserId, password);

      assignRealmRole(accessToken, keycloakUserId, DEFAULT_ROLE);

      return keycloakUserId;
    } catch (RuntimeException exception) {
      deleteUserSilently(accessToken, keycloakUserId);

      throw exception;
    }
  }

  public void deleteUser(String keycloakUserId) {
    String accessToken = tokenClient.createServiceAccessToken();

    try {
      deleteUser(accessToken, keycloakUserId);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        return;
      }

      throw new IdentityProviderException("Failed to delete Keycloak user", exception);
    } catch (RestClientException exception) {
      throw new IdentityProviderException("Failed to communicate with Keycloak", exception);
    }
  }

  private String createUserRecord(String accessToken, KeycloakUserCreateRequest request) {
    try {
      ResponseEntity<Void> response = restClient.post().uri(properties.adminUsersUrl())
          .headers(headers -> headers.setBearerAuth(accessToken))
          .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();

      return extractUserId(response);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 409) {
        throw new UserAlreadyExistsException("User with this login or email already exists");
      }

      throw new IdentityProviderException("Failed to create Keycloak user", exception);
    } catch (RestClientException exception) {
      throw new IdentityProviderException("Failed to communicate with Keycloak", exception);
    }
  }

  private void setPassword(String accessToken, String keycloakUserId, String password) {
    KeycloakCredentialRequest credential = new KeycloakCredentialRequest("password", password,
        false);

    try {
      restClient.put().uri(properties.adminUserPasswordUrl(keycloakUserId))
          .headers(headers -> headers.setBearerAuth(accessToken))
          .contentType(MediaType.APPLICATION_JSON).body(credential).retrieve().toBodilessEntity();
    } catch (RestClientException exception) {
      throw new IdentityProviderException("Failed to set Keycloak user password", exception);
    }
  }

  private void assignRealmRole(String accessToken, String keycloakUserId, String roleName) {
    KeycloakRoleResponse role = getRealmRole(accessToken, roleName);

    try {
      restClient.post().uri(properties.adminUserRealmRolesUrl(keycloakUserId))
          .headers(headers -> headers.setBearerAuth(accessToken))
          .contentType(MediaType.APPLICATION_JSON).body(List.of(role)).retrieve()
          .toBodilessEntity();
    } catch (RestClientException exception) {
      throw new IdentityProviderException("Failed to assign Keycloak realm role", exception);
    }
  }

  private KeycloakRoleResponse getRealmRole(String accessToken, String roleName) {
    try {
      KeycloakRoleResponse role = restClient.get().uri(properties.adminRealmRoleUrl(roleName))
          .headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
          .body(KeycloakRoleResponse.class);

      if (role == null || role.id() == null || role.name() == null) {
        throw new IdentityProviderException("Keycloak returned an invalid role response");
      }

      return role;
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new IdentityProviderException("Keycloak realm role was not found: " + roleName);
      }

      throw new IdentityProviderException("Failed to obtain Keycloak realm role", exception);
    } catch (RestClientException exception) {
      throw new IdentityProviderException("Failed to communicate with Keycloak", exception);
    }
  }

  private void deleteUser(String accessToken, String keycloakUserId) {
    restClient.delete().uri(properties.adminUserUrl(keycloakUserId))
        .headers(headers -> headers.setBearerAuth(accessToken)).retrieve().toBodilessEntity();
  }

  private void deleteUserSilently(String accessToken, String keycloakUserId) {
    try {
      deleteUser(accessToken, keycloakUserId);
    } catch (Exception exception) {
      log.error("Failed to compensate Keycloak user creation. " + "Keycloak user id: {}",
          keycloakUserId, exception);
    }
  }

  private String extractUserId(ResponseEntity<Void> response) {
    URI location = response.getHeaders().getLocation();

    if (location == null) {
      throw new IdentityProviderException("Keycloak did not return created user location");
    }

    String path = location.getPath();
    int lastSlashIndex = path.lastIndexOf('/');

    if (lastSlashIndex < 0 || lastSlashIndex == path.length() - 1) {
      throw new IdentityProviderException("Keycloak returned an invalid user location");
    }

    return path.substring(lastSlashIndex + 1);
  }
}
