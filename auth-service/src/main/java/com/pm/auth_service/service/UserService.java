package com.pm.auth_service.service;

import com.pm.auth_service.model.User;
import com.pm.auth_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        var user = userRepository.findByEmail(email);
        log.info("User with email {} found", user.toString());
        return user;
    }
}