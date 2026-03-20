package com.exercice.pmt.service;

import com.exercice.pmt.model.User;
import com.exercice.pmt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Tests unitaires")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");
    }

    // ---------------------------------------------------------------
    // userInscription()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("userInscription() - succès : l'utilisateur est sauvegardé")
    void userInscription_success() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.userInscription(user);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("userInscription() - échec : email déjà existant")
    void userInscription_emailAlreadyExists_throwsRuntimeException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.userInscription(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User with email already exists");

        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // login()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("login() - succès : email et mot de passe corrects")
    void login_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        User result = userService.login("john@example.com", "password123");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("login() - échec : email introuvable")
    void login_emailNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("unknown@example.com", "password123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Identifiants invalides");
    }

    @Test
    @DisplayName("login() - échec : mot de passe incorrect")
    void login_wrongPassword_throwsRuntimeException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login("john@example.com", "wrongPassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Identifiants invalides");
    }
}
