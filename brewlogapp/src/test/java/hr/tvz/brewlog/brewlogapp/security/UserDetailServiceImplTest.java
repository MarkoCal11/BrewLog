package hr.tvz.brewlog.brewlogapp.security;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UserDetailServiceImpl}: maps the domain {@link User} to a Spring
 * {@code UserDetails}, including the {@code active → enabled} flag the auth filter checks and the
 * {@code ROLE_<role>} authority mapping.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserDetailServiceImpl service;

    private User user(String username, Role role, boolean active) {
        return User.builder()
                .username(username)
                .password("hashed")
                .role(role)
                .active(active)
                .build();
    }

    @Test
    void loadsActiveUserAsEnabledWithRoleAuthority() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user("bob", Role.USER, true)));

        UserDetails details = service.loadUserByUsername("bob");

        assertThat(details.getUsername()).isEqualTo("bob");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void mapsAdminRoleToRoleAdminAuthority() {
        when(userRepository.findByUsername("boss")).thenReturn(Optional.of(user("boss", Role.ADMIN, true)));

        UserDetails details = service.loadUserByUsername("boss");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void inactiveUserIsDisabled() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.of(user("ghost", Role.USER, false)));

        UserDetails details = service.loadUserByUsername("ghost");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void unknownUserThrows() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody");
    }
}
