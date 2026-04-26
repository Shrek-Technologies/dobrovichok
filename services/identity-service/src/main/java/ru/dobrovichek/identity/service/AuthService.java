package ru.dobrovichek.identity.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.contracts.PhoneNormalizer;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.identity.dto.AuthResponse;
import ru.dobrovichek.identity.dto.LoginRequest;
import ru.dobrovichek.identity.dto.RegisterRequest;
import ru.dobrovichek.identity.exception.AuthConflictException;
import ru.dobrovichek.identity.exception.AuthUnauthorizedException;
import ru.dobrovichek.identity.repository.IdentityUserCredentialRepository;
import ru.dobrovichek.identity.repository.IdentityUserProfileRepository;
import ru.dobrovichek.identity.entity.UserCredentialEntity;
import ru.dobrovichek.identity.entity.UserProfileEntity;
import ru.dobrovichek.jwt.JwtTokenIssuer;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final IdentityUserProfileRepository profileRepository;
    private final IdentityUserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer jwtTokenIssuer;

    public AuthService(
            IdentityUserProfileRepository profileRepository,
            IdentityUserCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenIssuer jwtTokenIssuer
    ) {
        this.profileRepository = profileRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenIssuer = jwtTokenIssuer;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserRole role = request.role() == null ? UserRole.WARD : request.role();
        String patronymic = blankToNull(request.patronymic());
        String phoneNorm = PhoneNormalizer.normalize(request.phone());
        if (phoneNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите корректный телефон");
        }
        if (credentialRepository.existsByPhoneNormalized(phoneNorm) || profileRepository.existsByPhone(phoneNorm)) {
            throw new AuthConflictException("Пользователь с таким телефоном уже существует");
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        UserProfileEntity profile = UserProfileEntity.forNewRegistration(
                id,
                role,
                request.firstName().trim(),
                request.lastName().trim(),
                patronymic,
                phoneNorm,
                now
        );
        try {
            profileRepository.save(profile);
            credentialRepository.save(UserCredentialEntity.create(
                    id,
                    passwordEncoder.encode(request.password()),
                    phoneNorm
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new AuthConflictException("Пользователь с таким телефоном уже существует");
        }
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String phoneNorm = PhoneNormalizer.normalize(request.phone());
        if (phoneNorm.isBlank()) {
            throw new AuthUnauthorizedException("Неверный телефон или пароль");
        }
        UserCredentialEntity credential = credentialRepository.findByPhoneNormalized(phoneNorm)
                .orElse(null);
        if (credential == null || !passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new AuthUnauthorizedException("Неверный телефон или пароль");
        }
        UserProfileEntity profile = profileRepository.findById(credential.getUserId()).orElse(null);
        if (profile == null) {
            throw new AuthUnauthorizedException("Неверный телефон или пароль");
        }
        return toResponse(profile);
    }

    private AuthResponse toResponse(UserProfileEntity profile) {
        return new AuthResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPatronymic(),
                PersonNameFormat.fullFormal(profile.getFirstName(), profile.getPatronymic(), profile.getLastName()),
                profile.getPhone(),
                profile.getRole(),
                jwtTokenIssuer.createAccessToken(profile.getId(), profile.getRole())
        );
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
