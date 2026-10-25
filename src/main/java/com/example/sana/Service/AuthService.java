package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Create a normal user account with a hashed password
    public void register(User user) {

        if (userRepository.findUserByEmail(user.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }

        user.setId(null);
        user.setRole("USER");
        String hash = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(hash);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }
}
