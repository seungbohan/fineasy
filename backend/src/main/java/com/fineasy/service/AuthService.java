package com.fineasy.service;

import com.fineasy.dto.request.LoginRequest;
import com.fineasy.dto.request.RefreshTokenRequest;
import com.fineasy.dto.request.SignupRequest;
import com.fineasy.dto.response.AuthResponse;
import com.fineasy.entity.UserEntity;
import com.fineasy.exception.AuthenticationException;
import com.fineasy.exception.DuplicateEntityException;
import com.fineasy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEntityException("User", "email", request.email());
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateEntityException("User", "nickname", request.nickname());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        UserEntity user = new UserEntity(
                null,
                request.email(),
                encodedPassword,
                request.nickname(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
        );

        try {
            UserEntity savedUser = userRepository.save(user);
            return buildAuthResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent signup detected for email={}: {}", request.email(), e.getMessage());

            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateEntityException("User", "email", request.email());
            }
            throw new DuplicateEntityException("User", "nickname", request.nickname());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.refreshToken())) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        if (!tokenProvider.isRefreshToken(request.refreshToken())) {
            throw new AuthenticationException("Token is not a refresh token");
        }

        long userId = tokenProvider.getUserIdFromToken(request.refreshToken());
        String email = tokenProvider.getEmailFromToken(request.refreshToken());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getNickname())
        );
    }
}
