package hr.tvz.brewlog.brewlogapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(exclude = {"user"})
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
