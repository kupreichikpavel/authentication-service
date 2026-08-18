package by.innowise.authenticationservice.service;

import by.innowise.authenticationservice.client.KeycloakAdminClient;
import by.innowise.authenticationservice.client.UserServiceClient;
import by.innowise.authenticationservice.dto.keycloak.KeycloakUserCreateRequest;
import by.innowise.authenticationservice.dto.request.SignUpRequestDto;
import by.innowise.authenticationservice.dto.response.RegistrationResponseDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceCreateRequestDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceUserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

  private final UserServiceClient userServiceClient;
  private final KeycloakAdminClient keycloakAdminClient;

  public RegistrationResponseDto signUp(
      SignUpRequestDto request
  ) {
    KeycloakUserCreateRequest keycloakRequest =
        new KeycloakUserCreateRequest(
            request.login(),
            request.email(),
            true,
            Map.of()
        );

    String keycloakUserId =
        keycloakAdminClient.createUser(
            keycloakRequest,
            request.password()
        );

    Long userId = null;

    try {
      UserServiceCreateRequestDto userServiceRequest =
          new UserServiceCreateRequestDto(
              request.name(),
              request.surname(),
              request.birthDate(),
              request.email()
          );

      UserServiceUserResponseDto createdUser =
          userServiceClient.createUser(userServiceRequest);

      userId = createdUser.id();

      keycloakAdminClient.updateUserAttributes(
          keycloakUserId,
          keycloakRequest,
          Map.of(
              "userId",
              List.of(userId.toString())
          )
      );

      return new RegistrationResponseDto(
          userId,
          request.login()
      );
    } catch (RuntimeException exception) {
      if (userId != null) {
        compensateUserProfile(
            userId,
            exception
        );
      }

      compensateKeycloakUser(
          keycloakUserId,
          exception
      );

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

  private void compensateKeycloakUser(
      String keycloakUserId,
      RuntimeException originalException
  ) {
    try {
      keycloakAdminClient.deleteUser(keycloakUserId);
    } catch (RuntimeException compensationException) {
      originalException.addSuppressed(
          compensationException
      );

      log.error(
          "Failed to compensate Keycloak user creation. "
              + "Keycloak user id: {}",
          keycloakUserId,
          compensationException
      );
    }
  }
}