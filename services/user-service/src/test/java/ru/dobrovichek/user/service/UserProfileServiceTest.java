package ru.dobrovichek.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.user.dto.CurrentUser;
import ru.dobrovichek.user.dto.RegisterDeviceRequest;
import ru.dobrovichek.user.dto.UpdateMyProfileRequest;
import ru.dobrovichek.user.entity.UserCredential;
import ru.dobrovichek.user.entity.UserProfile;
import ru.dobrovichek.user.exception.ConflictException;
import ru.dobrovichek.user.exception.UserProfileNotFoundException;
import ru.dobrovichek.user.repository.UserCredentialJpaRepository;
import ru.dobrovichek.user.repository.UserProfileJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2024-06-01T10:00:00Z");

    @Mock
    private UserProfileJpaRepository userProfileRepository;
    @Mock
    private UserCredentialJpaRepository userCredentialRepository;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new UserProfileService(userProfileRepository, userCredentialRepository, clock);
    }

    @Test
    void getOrCreate_createsWhenMissing() {
        UUID id = UUID.randomUUID();
        CurrentUser user = new CurrentUser(id, UserRole.WARD);
        when(userProfileRepository.findById(id)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile profile = service.getOrCreateCurrent(user);

        assertEquals(id, profile.getId());
        assertEquals(UserRole.WARD, profile.getRole());
        assertEquals(NOW, profile.getCreatedAt());
    }

    @Test
    void getOrCreate_conflictOnRoleMismatch() {
        UUID id = UUID.randomUUID();
        UserProfile existing = UserProfile.create(id, UserRole.VOLUNTEER, NOW);
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> service.getOrCreateCurrent(new CurrentUser(id, UserRole.WARD)));
    }

    @Test
    void upsert_updatesCredentialPhoneWhenPresent() {
        UUID id = UUID.randomUUID();
        UserProfile profile = UserProfile.create(id, UserRole.WARD, NOW);
        UserCredential credential = UserCredential.create(id, "hash", "old");
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCredentialRepository.findById(id)).thenReturn(Optional.of(credential));
        when(userCredentialRepository.save(any(UserCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMyProfileRequest req = new UpdateMyProfileRequest(
                "Иван", "Иванов", null, "+7 (999) 111-22-33", null, null);

        UserProfile out = service.upsertCurrent(new CurrentUser(id, UserRole.WARD), req);

        assertEquals("+79991112233", out.getPhone());
        assertEquals("+79991112233", credential.getPhoneNormalized());
        verify(userCredentialRepository).save(credential);
    }

    @Test
    void registerDevice_trimsBlankToNull() {
        UUID id = UUID.randomUUID();
        UserProfile profile = UserProfile.create(id, UserRole.WARD, NOW);
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerDevice(new CurrentUser(id, UserRole.WARD), new RegisterDeviceRequest("   "));

        assertEquals(null, profile.getFcmToken());
    }

    @Test
    void findFcmToken_emptyWhenBlankStored() {
        UUID id = UUID.randomUUID();
        UserProfile profile = UserProfile.create(id, UserRole.WARD, NOW);
        profile.updateFcmToken("   ", NOW);
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        assertTrue(service.findFcmToken(id).isEmpty());
    }

    @Test
    void getVolunteerProfile_notFoundWhenWrongRole() {
        UUID id = UUID.randomUUID();
        UserProfile profile = UserProfile.create(id, UserRole.WARD, NOW);
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        assertThrows(UserProfileNotFoundException.class, () -> service.getVolunteerProfile(id));
    }

    @Test
    void getOrCreateVolunteerShell_createsShell() {
        UUID id = UUID.randomUUID();
        when(userProfileRepository.findById(id)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile out = service.getOrCreateVolunteerShell(id, NOW);

        assertEquals(UserRole.VOLUNTEER, out.getRole());
        assertEquals(id, out.getId());
    }

    @Test
    void ensureVolunteerExists_delegatesToGetVolunteerProfile() {
        UUID id = UUID.randomUUID();
        when(userProfileRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserProfileNotFoundException.class, () -> service.ensureVolunteerExists(id));
        verify(userProfileRepository, never()).save(any());
    }
}
