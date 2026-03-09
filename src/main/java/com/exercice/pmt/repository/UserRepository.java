package com.exercice.pmt.repository;

import com.exercice.pmt.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository {

    Optional<User> findByEmail(String email);

    User save(User user);
}
