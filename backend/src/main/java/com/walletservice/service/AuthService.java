package com.walletservice.service;

import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.dto.response.AuthResponse;
import com.walletservice.dto.response.RegisterResponse;
import com.walletservice.entity.User;
import com.walletservice.entity.Wallet;
import com.walletservice.exception.DuplicateEmailException;
import com.walletservice.exception.InvalidCredentialsException;
import com.walletservice.repository.UserRepository;
import com.walletservice.repository.WalletRepository;
import com.walletservice.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration attempt rejected: duplicate email {}", normalizedEmail);
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet(savedUser, "INR");
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Registered new user id={} with wallet id={}", savedUser.getId(), savedWallet.getId());
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedWallet.getId(),
                savedWallet.getCurrency(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: password mismatch for email {}", normalizedEmail);
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId());
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        log.info("User id={} logged in successfully", user.getId());
        return new AuthResponse("Bearer", token, expiresInSeconds);
    }
}
