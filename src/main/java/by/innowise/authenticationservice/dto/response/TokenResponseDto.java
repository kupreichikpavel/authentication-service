package by.innowise.authenticationservice.dto.response;

public record TokenResponseDto(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn,
        String tokenType
) {
}
