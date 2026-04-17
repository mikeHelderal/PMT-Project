package com.exercice.pmt.service;

import com.exercice.pmt.model.User;
import com.exercice.pmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service gérant le cycle de vie des utilisateurs et la sécurité des accès.
 * Ce service assure l'inscription des nouveaux membres et la validation
 * des informations d'authentification lors de la connexion.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Enregistre un nouvel utilisateur dans la base de données après vérification d'unicité.
     * * @param user L'entité utilisateur à créer
     * @return L'utilisateur sauvegardé avec son identifiant unique
     * * @throws RuntimeException si l'adresse email est déjà utilisée par un autre compte
     */
    public User userInscription(User user){
        System.out.println("userInscription => "+ user);
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new RuntimeException("User with email already exists");
        }


        return userRepository.save(user);
    }

    /**
     * Valide les identifiants d'un utilisateur pour autoriser l'accès à l'application.
     * * @param email Adresse email de connexion
     * * @param password Mot de passe associé
     * @return L'entité User complète en cas de succès
     * * @throws RuntimeException si l'email n'existe pas ou si le mot de passe est incorrect
     */
    public User login(String email, String password){
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow( () -> new RuntimeException("Identifiants invalides"));
    }
}
