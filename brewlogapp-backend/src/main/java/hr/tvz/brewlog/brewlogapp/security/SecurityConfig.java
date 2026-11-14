package hr.tvz.brewlog.brewlogapp.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final GooglePromptRequestResolver googlePromptRequestResolver;

    private static final String API_FLAVOR_NOTES_PATH = "/api/flavorNotes/**";
    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .securityContext(ctx -> ctx.securityContextRepository(new NullSecurityContextRepository()))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                            .requestMatchers(API_FLAVOR_NOTES_PATH).authenticated()
                            .requestMatchers("/api/admin/**").hasRole(ADMIN)
                            .requestMatchers("/api/coffeeBeans/**").authenticated()
                            .requestMatchers("/api/brewEntries/**").authenticated()
                            .requestMatchers("/api/reports/**").authenticated()
                            .requestMatchers("/uploads/**").permitAll()
                            .anyRequest().authenticated();
                })
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2SuccessHandler)
                    .authorizationEndpoint(auth -> auth
                            .authorizationRequestResolver(googlePromptRequestResolver)))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;
}
