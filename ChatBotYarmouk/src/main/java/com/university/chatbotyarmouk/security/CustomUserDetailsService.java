package com.university.chatbotyarmouk.security;

import com.university.chatbotyarmouk.entity.User;
import com.university.chatbotyarmouk.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Spring Security calls this when it needs to load a user
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // username here = email (best practice)
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convert user role to GrantedAuthority
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(user.getRole())); // ex: "ROLE_STUDENT"

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}