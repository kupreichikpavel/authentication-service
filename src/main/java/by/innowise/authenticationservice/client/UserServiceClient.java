package by.innowise.authenticationservice.client;

import by.innowise.authenticationservice.config.UserServiceProperties;
import by.innowise.authenticationservice.dto.userservice.UserServiceCreateRequestDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceUserResponseDto;
import by.innowise.authenticationservice.exception.InvalidUserProfileException;
import by.innowise.authenticationservice.exception.UserAlreadyExistsException;
import by.innowise.authenticationservice.exception.UserServiceCommunicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;
    private final UserServiceProperties properties;
    private final KeycloakTokenClient keycloakTokenClient;

    public UserServiceUserResponseDto createUser(
            UserServiceCreateRequestDto request
    ) {
        try {
            String serviceToken =
                    keycloakTokenClient.createServiceAccessToken();

            UserServiceUserResponseDto response = restClient
                    .post()
                    .uri(properties.usersUrl())
                    .headers(headers ->
                            headers.setBearerAuth(serviceToken)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(UserServiceUserResponseDto.class);

            validateUserResponse(response);

            return response;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 409) {
                throw new UserAlreadyExistsException(
                        "User with this email already exists"
                );
            }

            if (status == 400) {
                throw new InvalidUserProfileException();
            }

            throw new UserServiceCommunicationException(
                    "User Service failed to create a user",
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserServiceCommunicationException(
                    "User Service is unavailable",
                    exception
            );
        }
    }

    public void deleteUser(Long userId) {
        try {
            String serviceToken =
                    keycloakTokenClient.createServiceAccessToken();

            restClient
                    .delete()
                    .uri(properties.userUrl(userId))
                    .headers(headers ->
                            headers.setBearerAuth(serviceToken)
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return;
            }

            throw new UserServiceCommunicationException(
                    "User Service failed to delete a user",
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserServiceCommunicationException(
                    "User Service is unavailable",
                    exception
            );
        }
    }

    private void validateUserResponse(
            UserServiceUserResponseDto response
    ) {
        if (response == null || response.id() == null) {
            throw new UserServiceCommunicationException(
                    "User Service returned an invalid response"
            );
        }
    }
}
