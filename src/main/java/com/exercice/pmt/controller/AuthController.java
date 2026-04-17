package com.exercice.pmt.controller;

import com.exercice.pmt.model.User;
import com.exercice.pmt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur gérant les flux d'authentification et de sécurité
 * permet l'inscription de nouveaux utilisateurs et la validation des connexions
 */
@RestController
@RequestMapping("api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Enregistre un nouvel utilisateur dans le système.
     * * @param user Les données de l'utilisateur à créer (email, mot de passe, nom)
     * @return L'utilisateur créé avec son identifiant unique
     */
    @PostMapping("register")
    public User register(@RequestBody User user) {
        return userService.userInscription(user);
    }

    /**
     * Authentifie un utilisateur existant via ses identifiants.
     * * @param user Objet contenant l'email et le mot de passe de connexion
     * @return L'utilisateur authentifié si les credentials sont valides.
     * @throws RuntimeException si l'email ou le mot de passe est incorrect*/
    @PostMapping("login")
    public User login(@RequestBody User user) {
        return userService.login(user.getEmail(), user.getPassword());
    }
}
