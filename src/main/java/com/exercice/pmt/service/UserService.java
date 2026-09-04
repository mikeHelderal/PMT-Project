package com.exercice.pmt.service;

import com.exercice.pmt.model.User;
import com.exercice.pmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion des comptes utilisateurs : inscription et authentification.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Inscrit un nouvel utilisateur sur la plateforme.
     *
     * @param user compte à créer (username, e-mail, mot de passe)
     * @return l'utilisateur persisté, avec son identifiant
     * @throws ResponseStatusException 409 si l'adresse e-mail est déjà utilisée
     */
    public User userInscription(User user){
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Un compte existe déjà avec cette adresse e-mail");
        }

        return userRepository.save(user);
    }

    /**
     * Authentifie un utilisateur par e-mail et mot de passe.
     *
     * @param email    adresse e-mail saisie
     * @param password mot de passe saisi
     * @return l'utilisateur correspondant
     * @throws ResponseStatusException 401 si le couple e-mail / mot de passe est invalide
     */
    public User login(String email, String password){
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow( () -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Identifiants invalides"));
    }
}
