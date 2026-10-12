package hr.tvz.brewlog.brewlogapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Auth response. {@code refreshToken} is only populated for non-browser ("mobile") clients that
 * cannot use the HttpOnly refresh cookie; it is omitted from the JSON for web clients, who keep
 * receiving the refresh token via the Set-Cookie header only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JwtResponse(String accessToken, AuthUserResponse user, String refreshToken) {}
