package com.exercice.pmt.service;

import com.exercice.pmt.model.User;
import com.exercice.pmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User userInscription(User user){
        System.out.println("userInscription => "+ user);
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new RuntimeException("User with email already exists");
        }


        return userRepository.save(user);
    }

    public User login(String email, String password){
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow( () -> new RuntimeException("Identifiants invalides"));
    }
}
