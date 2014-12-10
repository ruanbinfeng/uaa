package org.cloudfoundry.identity.oauth2showcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.security.oauth2.sso.EnableOAuth2Sso;
import org.springframework.cloud.security.oauth2.sso.OAuth2SsoConfigurerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableConfigurationProperties
@EnableOAuth2Sso
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConfigurationProperties(prefix = "oauth_clients.client_credentials")
    OAuth2ProtectedResourceDetails clientCredentialsResourceDetails() {
        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public OAuth2RestTemplate uaaClientCredentialsRestTemplate(OAuth2ClientContext oauth2Context) {
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientCredentialsResourceDetails(), oauth2Context);
        return restTemplate;
    }

    @Bean
    OAuth2SsoConfigurerAdapter oAuth2SsoConfigurerAdapter() {
        return new OAuth2SsoConfigurerAdapter() {
            @Override
            public void match(RequestMatchers matchers) {
                matchers.antMatchers("/authorization_code/**");
            }

            public void configure(HttpSecurity http) throws Exception {
                http.authorizeRequests()
                    .antMatchers("/").permitAll();
            }
//            public void match(RequestMatchers matchers) {
//                matchers.antMatchers("/dashboard/**");
//            }
//
//            @Override
//            public void configure(HttpSecurity http) throws Exception {
//                http.authorizeRequests().anyRequest().authenticated();
//            }
        };
    }

//        @Configuration
//        protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
//    
//            @Override
//            protected void configure(HttpSecurity http) throws Exception {
//                http.authorizeRequests().antMatchers("/anonymous").permitAll();
//            }
//    
//        }

}