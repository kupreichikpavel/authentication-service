package by.innowise.authenticationservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.user-service")
public record UserServiceProperties(

        @NotBlank
        String baseUrl

) {

    public String usersUrl() {
        return normalizedBaseUrl() + "/api/v1/users";
    }

    public String userUrl(Long userId) {
        return usersUrl() + "/" + userId;
    }

    private String normalizedBaseUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(
                    0,
                    baseUrl.length() - 1
            );
        }

        return baseUrl;
    }

}
