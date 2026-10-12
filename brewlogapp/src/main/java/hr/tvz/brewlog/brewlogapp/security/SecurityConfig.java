package hr.tvz.brewlog.brewlogapp.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
            // IF_REQUIRED so Spring can create a session during the OAuth2 redirect dance
            // (state parameter storage). JWT filter still authenticates all API requests
            // independently without touching the session.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // Never load or save the security context from/to the HTTP session.
            // The OAuth2 dance still works (it stores only the state param in the session,
            // which is separate), but the OAuth2 auth token is never persisted to the session
            // so it cannot bleed into JWT-authenticated API calls.
            .securityContext(ctx -> ctx.securityContextRepository(new NullSecurityContextRepository()))
            .authorizeHttpRequests(auth -> auth
                    // Only authorize the initial REQUEST dispatch. The custom JWT filter
                    // (OncePerRequestFilter) skips ERROR/ASYNC dispatches, so re-authorizing
                    // the /error forward would clobber every 400/403/500 into a 401.
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers(API_FLAVOR_NOTES_PATH).authenticated()
                    .requestMatchers("/api/admin/**").hasRole(ADMIN)
                    .requestMatchers("/api/coffeeBeans/**").authenticated()
                    .requestMatchers("/api/brewEntries/**").authenticated()
                    .requestMatchers("/api/reports/**").authenticated()
                    .requestMatchers("/uploads/**").permitAll()
                    .anyRequest().authenticated()
            )
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
}
