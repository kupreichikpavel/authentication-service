package by.innowise.authenticationservice.service;

import by.innowise.authenticationservice.client.KeycloakTokenClient;
import by.innowise.authenticationservice.dto.keycloak.KeycloakTokenResponse;
import by.innowise.authenticationservice.dto.request.LoginRequestDto;
import by.innowise.authenticationservice.dto.request.RefreshTokenRequestDto;
import by.innowise.authenticationservice.dto.request.TokenValidationRequestDto;
import by.innowise.authenticationservice.dto.response.TokenResponseDto;
import by.innowise.authenticationservice.dto.response.TokenValidationResponseDto;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final KeycloakTokenClient keycloakTokenClient;

  public TokenService(KeycloakTokenClient keycloakTokenClient) {
    this.keycloakTokenClient = keycloakTokenClient;
  }

  public TokenResponseDto createToken(LoginRequestDto request) {
    KeycloakTokenResponse response =
        keycloakTokenClient.createUserToken(
            request.login(),
            request.password()
        );

    return toTokenResponse(response);
  }

  public TokenResponseDto refreshToken(
      RefreshTokenRequestDto request
  ) {
    KeycloakTokenResponse response =
        keycloakTokenClient.refreshUserToken(
            request.refreshToken()
        );

    return toTokenResponse(response);
  }

  public TokenValidationResponseDto validateToken(
      TokenValidationRequestDto request
  ) {
    boolean valid = keycloakTokenClient
        .introspect(request.token())
        .active();

    return new TokenValidationResponseDto(valid);
  }

  private TokenResponseDto toTokenResponse(
      KeycloakTokenResponse response
  ) {
    return new TokenResponseDto(
        response.accessToken(),
        response.refreshToken(),
        response.expiresIn(),
        response.refreshExpiresIn(),
        response.tokenType()
    );
  }
}
