package hr.tvz.brewlog.brewlogapp.exception;

import hr.tvz.brewlog.brewlogapp.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Translates service-layer exceptions into a uniform JSON body {@code { "message": "..." }}
 * with a consistent status code, so the frontend can always surface a readable message.
 *
 * <ul>
 *   <li>{@link DuplicateResourceException} (name collisions) &rarr; 409 Conflict</li>
 *   <li>{@link IllegalStateException} (e.g. bean still has brew entries, last-admin guard) &rarr; 409 Conflict</li>
 *   <li>{@link IllegalArgumentException} (bad input, unknown referenced id, bad upload) &rarr; 400 Bad Request</li>
 * </ul>
 *
 * Note: {@code DuplicateResourceException} extends {@code IllegalArgumentException}, so its
 * handler must be declared first / matched more specifically — Spring resolves the most
 * specific {@code @ExceptionHandler} for the thrown type, which is the duplicate handler here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("Image is too large. Please use a file under 15 MB."));
    }
}
