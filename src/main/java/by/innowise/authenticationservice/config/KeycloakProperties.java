package by.innowise.authenticationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(
    String serverUrl,
    String realm,
    String clientId,
    String clientSecret
) {

  public KeycloakProperties {
    serverUrl = requireText(serverUrl, "server-url");
    realm = requireText(realm, "realm");
    clientId = requireText(clientId, "client-id");
    clientSecret = requireText(clientSecret, "client-secret");
  }

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

  private static String requireText(
      String value,
      String propertyName
  ) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "app.keycloak."
              + propertyName
              + " must not be blank"
      );
    }

    return value;
  }
}
