package by.innowise.authenticationservice.service;

import by.innowise.authenticationservice.client.KeycloakAdminClient;
import by.innowise.authenticationservice.client.UserServiceClient;
import by.innowise.authenticationservice.dto.keycloak.KeycloakUserCreateRequest;
import by.innowise.authenticationservice.dto.request.RegistrationRequestDto;
import by.innowise.authenticationservice.dto.response.RegistrationResponseDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceCreateRequestDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceUserResponseDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Logger log =
            LoggerFactory.getLogger(RegistrationService.class);

    private final UserServiceClient userServiceClient;
    private final KeycloakAdminClient keycloakAdminClient;

    public RegistrationResponseDto register(
            RegistrationRequestDto request
    ) {
        UserServiceCreateRequestDto userServiceRequest =
                new UserServiceCreateRequestDto(
                        request.name(),
                        request.surname(),
                        request.birthDate(),
                        request.email()
                );

        UserServiceUserResponseDto createdUser =
                userServiceClient.createUser(userServiceRequest);

        Long userId = createdUser.id();

        try {
            KeycloakUserCreateRequest keycloakRequest =
                    new KeycloakUserCreateRequest(
                            request.login(),
                            request.email(),
                            request.name(),
                            request.surname(),
                            true,
                            Map.of(
                                    "userId",
                                    List.of(userId.toString())
                            )
                    );

            keycloakAdminClient.createUser(
                    keycloakRequest,
                    request.password()
            );

            return new RegistrationResponseDto(
                    userId,
                    request.login()
            );
        } catch (RuntimeException exception) {
            compensateUserProfile(userId, exception);
            throw exception;
        }
    }

    private void compensateUserProfile(
            Long userId,
            RuntimeException originalException
    ) {
        try {
            userServiceClient.deleteUser(userId);
        } catch (RuntimeException compensationException) {
            originalException.addSuppressed(
                    compensationException
            );

            log.error(
                    "Failed to compensate User Service profile creation. "
                            + "User id: {}",
                    userId,
                    compensationException
            );
        }
    }
}
