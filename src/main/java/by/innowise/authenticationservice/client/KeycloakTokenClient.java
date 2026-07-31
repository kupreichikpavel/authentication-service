package by.innowise.authenticationservice.client;

import by.innowise.authenticationservice.config.KeycloakProperties;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import by.innowise.authenticationservice.dto.keycloak.KeycloakIntrospectionResponse;
import by.innowise.authenticationservice.dto.keycloak.KeycloakTokenResponse;
import by.innowise.authenticationservice.exception.IdentityProviderException;
import by.innowise.authenticationservice.exception.InvalidAuthenticationDataException;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KeycloakTokenClient {

    private static final String INVALID_GRANT_ERROR = "invalid_grant";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakTokenClient(
            RestClient restClient,
            KeycloakProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KeycloakTokenResponse createUserToken(
            String login,
            String password
    ) {
        MultiValueMap<String, String> form =
                createClientCredentialsForm();

        form.add("grant_type", "password");
        form.add("username", login);
        form.add("password", password);

        return sendUserTokenRequest(form);
    }

    public KeycloakTokenResponse refreshUserToken(
            String refreshToken
    ) {
        MultiValueMap<String, String> form =
                createClientCredentialsForm();

        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return sendUserTokenRequest(form);
    }

    public KeycloakIntrospectionResponse introspect(String token) {
        MultiValueMap<String, String> form =
                createClientCredentialsForm();

        form.add("token", token);

        try {
            KeycloakIntrospectionResponse response = restClient.post()
                    .uri(properties.introspectionUrl())
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(form)
                    .retrieve()
                    .body(KeycloakIntrospectionResponse.class);

            if (response == null) {
                throw new IdentityProviderException(
                        "Keycloak returned an empty introspection response"
                );
            }

            return response;
        } catch (IdentityProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new IdentityProviderException(
                    "Failed to validate token",
                    exception
            );
        }
    }

    private KeycloakTokenResponse sendUserTokenRequest(
            MultiValueMap<String, String> form
    ) {
        try {
            return sendTokenRequest(form);
        } catch (HttpClientErrorException.BadRequest exception) {
            if (isInvalidGrant(exception)) {
                throw new InvalidAuthenticationDataException(
                        "Invalid login, password or refresh token"
                );
            }

            throw new IdentityProviderException(
                    "Keycloak rejected the token request",
                    exception
            );
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new IdentityProviderException(
                    "Keycloak client authentication failed",
                    exception
            );
        } catch (RestClientException exception) {
            throw new IdentityProviderException(
                    "Failed to communicate with Keycloak",
                    exception
            );
        }
    }

    private KeycloakTokenResponse sendTokenRequest(
            MultiValueMap<String, String> form
    ) {
        KeycloakTokenResponse response = restClient.post()
                .uri(properties.tokenUrl())
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                )
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IdentityProviderException(
                    "Keycloak returned an invalid token response"
            );
        }

        return response;
    }

    public String createServiceAccessToken() {
        LinkedMultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        try {
            KeycloakTokenResponse response = restClient
                    .post()
                    .uri(properties.tokenUrl())
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null
                    || response.accessToken() == null
                    || response.accessToken().isBlank()) {
                throw new IdentityProviderException(
                        "Keycloak returned an invalid service token response"
                );
            }

            return response.accessToken();
        } catch (RestClientException exception) {
            throw new IdentityProviderException(
                    "Failed to obtain Keycloak service token",
                    exception
            );
        }
    }

    private MultiValueMap<String, String>
    createClientCredentialsForm() {
        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        return form;
    }

    private boolean isInvalidGrant(
            HttpClientErrorException.BadRequest exception
    ) {
        return exception
                .getResponseBodyAsString()
                .contains(INVALID_GRANT_ERROR);
    }
}
