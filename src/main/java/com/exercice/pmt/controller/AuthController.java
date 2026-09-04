package com.exercice.pmt.controller;

import com.exercice.pmt.model.User;
import com.exercice.pmt.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Points d'entrée d'inscription et de connexion à la plateforme.
 */
@RestController
@RequestMapping("api/auth")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Authentification", description = "Inscription et connexion des utilisateurs")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Inscrit un nouveau visiteur sur la plateforme.
     *
     * @param user compte à créer : username, email et password
     * @return l'utilisateur créé, avec son identifiant
     */
    @Operation(
            summary = "Inscrire un nouvel utilisateur",
            description = """
                    Crée un compte à partir d'un nom d'utilisateur, d'une adresse e-mail et
                    d'un mot de passe. L'adresse e-mail doit être unique sur la plateforme.

                    Exemple de corps de requête :
                    ```json
                    { "username": "Paul Martin", "email": "paul@pmt.com", "password": "secret" }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte créé"),
            @ApiResponse(responseCode = "409", description = "Un compte existe déjà avec cette adresse e-mail")
    })
    @PostMapping("register")
    public User register(@RequestBody User user) {
        return userService.userInscription(user);
    }

    /**
     * Authentifie un utilisateur déjà inscrit.
     *
     * @param user identifiants de connexion : email et password
     * @return l'utilisateur authentifié
     */
    @Operation(
            summary = "Connecter un utilisateur",
            description = """
                    Authentifie un inscrit à partir de son adresse e-mail et de son mot de passe.

                    Exemple de corps de requête :
                    ```json
                    { "email": "admin@pmt.com", "password": "admin123" }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie ; l'utilisateur est retourné"),
            @ApiResponse(responseCode = "401", description = "Adresse e-mail ou mot de passe invalide")
    })
    @PostMapping("login")
    public User login(@RequestBody User user) {
        return userService.login(user.getEmail(), user.getPassword());
    }
}
