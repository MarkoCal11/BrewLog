package hr.tvz.brewlog.brewlogapp.security;

public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }
}
