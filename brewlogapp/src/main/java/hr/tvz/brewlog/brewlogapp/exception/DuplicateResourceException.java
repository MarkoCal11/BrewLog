package hr.tvz.brewlog.brewlogapp.exception;

/**
 * Thrown when creating or renaming a resource would collide with an existing one
 * (e.g. a duplicate flavor-note or coffee-bean name). Extends {@link IllegalArgumentException}
 * so existing service tests that assert on {@code IllegalArgumentException} keep passing, while
 * the global handler can map it to {@code 409 Conflict} rather than the generic {@code 400}.
 */
public class DuplicateResourceException extends IllegalArgumentException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
