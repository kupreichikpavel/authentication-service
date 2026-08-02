package by.innowise.authenticationservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(

    @NotBlank
    String serverUrl,

    @NotBlank
    String realm,

    @NotBlank
    String clientId,

    @NotBlank
    String clientSecret

) {

  public String tokenUrl() {
    return normalizedServerUrl()
        + "/realms/"
        + realm
        + "/protocol/openid-connect/token";
  }

  public String introspectionUrl() {
    return normalizedServerUrl()
        + "/realms/"
        + realm
        + "/protocol/openid-connect/token/introspect";
  }

  public String adminUsersUrl() {
    return normalizedServerUrl()
        + "/admin/realms/"
        + realm
        + "/users";
  }

  public String adminUserUrl(String keycloakUserId) {
    return adminUsersUrl() + "/" + keycloakUserId;
  }

  public String adminUserPasswordUrl(String keycloakUserId) {
    return adminUserUrl(keycloakUserId) + "/reset-password";
  }

  public String adminRealmRoleUrl(String roleName) {
    return normalizedServerUrl()
        + "/admin/realms/"
        + realm
        + "/roles/"
        + roleName;
  }

  public String adminUserRealmRolesUrl(String keycloakUserId) {
    return adminUserUrl(keycloakUserId)
        + "/role-mappings/realm";
  }

  private String normalizedServerUrl() {
    if (serverUrl.endsWith("/")) {
      return serverUrl.substring(
          0,
          serverUrl.length() - 1
      );
    }

    return serverUrl;
  }
}
