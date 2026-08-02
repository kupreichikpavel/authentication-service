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

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerShouldReturnCreatedUser() {
        SignUpRequestDto request = request();

        when(userServiceClient.createUser(
                any(UserServiceCreateRequestDto.class)
        )).thenReturn(
                new UserServiceUserResponseDto(USER_ID)
        );

        when(keycloakAdminClient.createUser(
                any(KeycloakUserCreateRequest.class),
                eq(request.password())
        )).thenReturn("keycloak-user-id");

        RegistrationResponseDto response =
                registrationService.register(request);

        assertEquals(USER_ID, response.userId());
        assertEquals(request.login(), response.login());

        verify(userServiceClient, never())
                .deleteUser(USER_ID);
    }

    @Test
    void registerShouldDeleteProfileWhenKeycloakFails() {
        SignUpRequestDto request = request();

        when(userServiceClient.createUser(
                any(UserServiceCreateRequestDto.class)
        )).thenReturn(
                new UserServiceUserResponseDto(USER_ID)
        );

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

        verify(userServiceClient)
                .deleteUser(USER_ID);
    }

    @Test
    void registerShouldNotCallKeycloakWhenProfileCreationFails() {
        SignUpRequestDto request = request();

        when(userServiceClient.createUser(
                any(UserServiceCreateRequestDto.class)
        )).thenThrow(
                new UserAlreadyExistsException()
        );

        assertThrows(
                UserAlreadyExistsException.class,
                () -> registrationService.register(request)
        );

        verifyNoInteractions(keycloakAdminClient);

        verify(userServiceClient, never())
                .deleteUser(USER_ID);
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
