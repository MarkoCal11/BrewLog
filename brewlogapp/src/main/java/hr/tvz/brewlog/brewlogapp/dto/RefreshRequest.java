package hr.tvz.brewlog.brewlogapp.dto;

/**
 * Optional body for /api/auth/refresh and /api/auth/logout used by non-browser ("mobile") clients
 * that hold the refresh token themselves instead of in the HttpOnly cookie. Web clients send no
 * body and the token is read from the cookie.
 */
public record RefreshRequest(String refreshToken) {}
