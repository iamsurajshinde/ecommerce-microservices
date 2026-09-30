package com.ecommerce.userservice.service;

import com.ecommerce.userservice.event.EventPublisher;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link UserService#saveUser(User)} publishes a {@code user.registered}
 * event with the correct payload after a successful save, and does not publish when
 * registration is rejected.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private EventPublisher eventPublisher;

    private UserService newUserService() {
        return new UserService(userRepository, new BCryptPasswordEncoder(), cacheManager, eventPublisher);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveUserPublishesUserRegisteredEventWithPayload() {
        UserService userService = newUserService();

        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(42L);
            return toSave;
        });

        User input = User.builder()
                .name("Ada Lovelace")
                .email("Ada@Example.com")
                .password("secret123")
                .role("USER")
                .build();

        userService.saveUser(input);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq(EventPublisher.RK_USER_REGISTERED), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("userId")).isEqualTo(42L);
        assertThat(payload.get("email")).isEqualTo("ada@example.com"); // normalized to lower-case
        assertThat(payload.get("name")).isEqualTo("Ada Lovelace");
        assertThat(payload.get("eventId")).asString()
                .startsWith("user-registered-42-")
                .isNotBlank();
    }

    @Test
    void saveUserDoesNotPublishWhenEmailAlreadyInUse() {
        UserService userService = newUserService();
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        User input = User.builder()
                .name("Dup")
                .email("dup@example.com")
                .password("secret123")
                .role("USER")
                .build();

        try {
            userService.saveUser(input);
        } catch (IllegalStateException expected) {
            // expected: duplicate email
        }

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void saveUserDoesNotPublishWhenEmailInvalid() {
        UserService userService = newUserService();

        User input = User.builder()
                .name("Bad Email")
                .email("not-an-email")
                .password("secret123")
                .role("USER")
                .build();

        try {
            userService.saveUser(input);
        } catch (IllegalArgumentException expected) {
            // expected: invalid email
        }

        verify(eventPublisher, never()).publish(anyString(), any());
    }
}
