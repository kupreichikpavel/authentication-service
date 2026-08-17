package by.innowise.authenticationservice.service;

import by.innowise.authenticationservice.client.KeycloakAdminClient;
import by.innowise.authenticationservice.client.UserServiceClient;
import by.innowise.authenticationservice.dto.keycloak.KeycloakUserCreateRequest;
import by.innowise.authenticationservice.dto.request.SignUpRequestDto;
import by.innowise.authenticationservice.dto.response.RegistrationResponseDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceCreateRequestDto;
import by.innowise.authenticationservice.dto.userservice.UserServiceUserResponseDto;
import by.innowise.authenticationservice.exception.IdentityProviderException;
import by.innowise.authenticationservice.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  private static final Long USER_ID = 42L;
  private static final String KEYCLOAK_USER_ID = "keycloak-user-id";

  @Mock
  private UserServiceClient userServiceClient;

  @Mock
  private KeycloakAdminClient keycloakAdminClient;

  @InjectMocks
  private RegistrationService registrationService;

  @Test
  void registerShouldReturnCreatedUser() {
    SignUpRequestDto request = request();
    KeycloakUserCreateRequest keycloakRequest =
        keycloakRequest(request);

    when(keycloakAdminClient.createUser(
        any(KeycloakUserCreateRequest.class),
        eq(request.password())
    )).thenReturn(KEYCLOAK_USER_ID);

    when(userServiceClient.createUser(
        any(UserServiceCreateRequestDto.class)
    )).thenReturn(
        new UserServiceUserResponseDto(USER_ID)
    );

    RegistrationResponseDto response =
        registrationService.register(request);

    assertEquals(USER_ID, response.userId());
    assertEquals(request.login(), response.login());

    verify(keycloakAdminClient).updateUserAttributes(
        KEYCLOAK_USER_ID,
        keycloakRequest,
        Map.of(
            "userId",
            List.of(USER_ID.toString())
        )
    );

    verify(userServiceClient, never())
        .deleteUser(USER_ID);

    verify(keycloakAdminClient, never())
        .deleteUser(KEYCLOAK_USER_ID);
  }

  @Test
  void registerShouldDeleteKeycloakUserWhenProfileCreationFails() {
    SignUpRequestDto request = request();

    when(keycloakAdminClient.createUser(
        any(KeycloakUserCreateRequest.class),
        eq(request.password())
    )).thenReturn(KEYCLOAK_USER_ID);

    UserAlreadyExistsException expectedException =
        new UserAlreadyExistsException();

    when(userServiceClient.createUser(
        any(UserServiceCreateRequestDto.class)
    )).thenThrow(expectedException);

    UserAlreadyExistsException actualException =
        assertThrows(
            UserAlreadyExistsException.class,
            () -> registrationService.register(request)
        );

    assertSame(expectedException, actualException);

    verify(keycloakAdminClient)
        .deleteUser(KEYCLOAK_USER_ID);

    verify(keycloakAdminClient, never())
        .updateUserAttributes(
            any(),
            any(KeycloakUserCreateRequest.class),
            any()
        );

    verify(userServiceClient, never())
        .deleteUser(USER_ID);
  }

  @Test
  void registerShouldDeleteBothUsersWhenAttributeUpdateFails() {
    SignUpRequestDto request = request();

    when(keycloakAdminClient.createUser(
        any(KeycloakUserCreateRequest.class),
        eq(request.password())
    )).thenReturn(KEYCLOAK_USER_ID);

    when(userServiceClient.createUser(
        any(UserServiceCreateRequestDto.class)
    )).thenReturn(
        new UserServiceUserResponseDto(USER_ID)
    );

    IdentityProviderException expectedException =
        new IdentityProviderException(
            "Failed to update Keycloak user"
        );

    org.mockito.Mockito.doThrow(expectedException)
        .when(keycloakAdminClient)
        .updateUserAttributes(
            eq(KEYCLOAK_USER_ID),
            any(KeycloakUserCreateRequest.class),
            any()
        );

    IdentityProviderException actualException =
        assertThrows(
            IdentityProviderException.class,
            () -> registrationService.register(request)
        );

    assertSame(expectedException, actualException);

    verify(userServiceClient)
        .deleteUser(USER_ID);

    verify(keycloakAdminClient)
        .deleteUser(KEYCLOAK_USER_ID);
  }

  @Test
  void registerShouldNotCallUserServiceWhenKeycloakCreationFails() {
    SignUpRequestDto request = request();

    IdentityProviderException expectedException =
        new IdentityProviderException(
            "Keycloak error"
        );

    when(keycloakAdminClient.createUser(
        any(KeycloakUserCreateRequest.class),
        eq(request.password())
    )).thenThrow(expectedException);

    IdentityProviderException actualException =
        assertThrows(
            IdentityProviderException.class,
            () -> registrationService.register(request)
        );

    assertSame(expectedException, actualException);

    verifyNoInteractions(userServiceClient);

    verify(keycloakAdminClient, never())
        .deleteUser(any());
  }

  private KeycloakUserCreateRequest keycloakRequest(
      SignUpRequestDto request
  ) {
    return new KeycloakUserCreateRequest(
        request.login(),
        request.email(),
        request.name(),
        request.surname(),
        true,
        Map.of()
    );
  }

  private SignUpRequestDto request() {
    return new SignUpRequestDto(
        "registration-test",
        "TestPassword123!",
        "Pavel",
        "Kupreichik",
        LocalDate.of(2005, 1, 1),
        "registration.test@example.com"
    );
  }
}
