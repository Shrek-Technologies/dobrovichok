package ru.dobrovichek.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dobrovichek.contracts.PhoneNormalizer;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.user.api.CurrentUser;
import ru.dobrovichek.user.api.RegisterDeviceRequest;
import ru.dobrovichek.user.api.UpdateMyProfileRequest;
import ru.dobrovichek.user.domain.UserProfile;
import ru.dobrovichek.user.infrastructure.persistence.UserCredentialJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.UserProfileJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileJpaRepository userProfileRepository;
    private final UserCredentialJpaRepository userCredentialRepository;
    private final Clock clock;

    public UserProfileService(
            UserProfileJpaRepository userProfileRepository,
            UserCredentialJpaRepository userCredentialRepository,
            Clock clock
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.clock = clock;
    }

    @Transactional
    public UserProfile getOrCreateCurrent(CurrentUser currentUser) {
        return userProfileRepository.findById(currentUser.userId())
                .map(profile -> validateRole(profile, currentUser.role()))
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(
                        currentUser.userId(),
                        currentUser.role(),
                        Instant.now(clock)
                )));
    }

    @Transactional
    public UserProfile upsertCurrent(CurrentUser currentUser, UpdateMyProfileRequest request) {
        UserProfile profile = getOrCreateCurrent(currentUser);
        profile.updateProfile(
                request.firstName(),
                request.lastName(),
                request.patronymic(),
                request.phone(),
                request.bio(),
                request.city(),
                Instant.now(clock)
        );
        UserProfile saved = userProfileRepository.save(profile);
        userCredentialRepository.findById(saved.getId()).ifPresent(credential -> {
            credential.updatePhoneNormalized(PhoneNormalizer.normalize(saved.getPhone()));
            userCredentialRepository.save(credential);
        });
        return saved;
    }

    @Transactional
    public void registerDevice(CurrentUser currentUser, RegisterDeviceRequest request) {
        UserProfile profile = getOrCreateCurrent(currentUser);
        String raw = request.fcmToken();
        String token = raw == null ? null : raw.trim();
        if (token != null && token.isEmpty()) {
            token = null;
        }
        profile.updateFcmToken(token, Instant.now(clock));
        userProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Optional<String> findFcmToken(UUID userId) {
        return userProfileRepository.findById(userId)
                .map(UserProfile::getFcmToken)
                .filter(s -> s != null && !s.isBlank());
    }

    @Transactional(readOnly = true)
    public UserProfile getVolunteerProfile(UUID volunteerId) {
        UserProfile profile = userProfileRepository.findById(volunteerId)
                .orElseThrow(() -> new UserProfileNotFoundException(volunteerId));
        if (profile.getRole() != UserRole.VOLUNTEER) {
            throw new UserProfileNotFoundException(volunteerId);
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public void ensureVolunteerExists(UUID volunteerId) {
        getVolunteerProfile(volunteerId);
    }

    @Transactional
    public UserProfile getOrCreateVolunteerShell(UUID volunteerId, Instant now) {
        return userProfileRepository.findById(volunteerId)
                .map(profile -> validateRole(profile, UserRole.VOLUNTEER))
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(volunteerId, UserRole.VOLUNTEER, now)));
    }

    private UserProfile validateRole(UserProfile profile, UserRole expectedRole) {
        if (profile.getRole() != expectedRole) {
            throw new ConflictException("Profile role mismatch for user " + profile.getId());
        }
        return profile;
    }
}
