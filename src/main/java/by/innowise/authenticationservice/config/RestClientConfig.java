package by.innowise.authenticationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient restClient(
      @Value("${app.http.connect-timeout:3s}")
      Duration connectTimeout,
      @Value("${app.http.read-timeout:10s}")
      Duration readTimeout
  ) {
    SimpleClientHttpRequestFactory requestFactory =
        new SimpleClientHttpRequestFactory();

    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);

    return RestClient.builder()
        .requestFactory(requestFactory)
        .build();
  }
}
