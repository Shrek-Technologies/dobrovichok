package ru.dobrovichek.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.jwt.JwtProperties;
import ru.dobrovichek.jwt.JwtTokenIssuer;
import ru.dobrovichek.user.config.SecurityConfiguration;
import ru.dobrovichek.user.controller.RestExceptionHandler;
import ru.dobrovichek.user.controller.UserController;
import ru.dobrovichek.user.controller.WebMvcConfiguration;
import ru.dobrovichek.user.dto.VolunteerRequestHistoryResponse;
import ru.dobrovichek.user.entity.UserProfile;
import ru.dobrovichek.user.entity.VolunteerRating;
import ru.dobrovichek.user.exception.ConflictException;
import ru.dobrovichek.user.exception.ForbiddenException;
import ru.dobrovichek.user.service.UserProfileService;
import ru.dobrovichek.user.service.VolunteerRatingService;
import ru.dobrovichek.user.util.CurrentUserArgumentResolver;
import ru.dobrovichek.user.util.UserProfileMapper;
import ru.dobrovichek.user.util.VolunteerRequestHistoryAssembler;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({
        RestExceptionHandler.class,
        SecurityConfiguration.class,
        WebMvcConfiguration.class,
        CurrentUserArgumentResolver.class,
        UserProfileMapper.class
})
@TestPropertySource(properties = {
        "dobrovichek.jwt.secret=test-test-test-test-test-test-test-test-32bytes!",
        "dobrovichek.jwt.issuer=dobrovichek"
})
class UserControllerTest {

    private static final Instant T0 = Instant.parse("2026-04-23T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-04-23T13:00:00Z");
    private static final Instant T2 = Instant.parse("2026-04-23T14:00:00Z");
    private static final Instant T3 = Instant.parse("2026-04-23T15:00:00Z");

    private static final UUID VOLUNTEER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_WARD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID REQUEST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SECOND_REQUEST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private VolunteerRequestHistoryAssembler volunteerRequestHistoryAssembler;

    @MockBean
    private VolunteerRatingService volunteerRatingService;

    private String bearer(UUID userId, UserRole role) {
        return "Bearer " + new JwtTokenIssuer(jwtProperties).createAccessToken(userId, role);
    }

    private UserProfile volunteerShell() {
        return UserProfile.create(VOLUNTEER_ID, UserRole.VOLUNTEER, T0);
    }

    private UserProfile elenaProfile() {
        UserProfile p = volunteerShell();
        p.updateProfile("Elena", "Smirnova", "", "+79991111111", "Volunteer driver", "Saint Petersburg", T0);
        p.registerCompletedRequest(T1);
        p.registerCompletedRequest(T2);
        return p;
    }

    @Test
    void getMyProfileCreatesShellFromHeaders() throws Exception {
        when(userProfileService.getOrCreateCurrent(any())).thenReturn(UserProfile.create(WARD_ID, UserRole.WARD, T0));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(WARD_ID.toString()))
                .andExpect(jsonPath("$.role").value("WARD"))
                .andExpect(jsonPath("$.firstName").value(""))
                .andExpect(jsonPath("$.lastName").value(""))
                .andExpect(jsonPath("$.fullName").value(""));
    }

    @Test
    void putMyProfileUpdatesCurrentUserProfile() throws Exception {
        UserProfile updated = volunteerShell();
        updated.updateProfile("Ivan", "Petrov", "", "+79990000000", "Experienced volunteer", "Moscow", T1);
        when(userProfileService.upsertCurrent(any(), any())).thenReturn(updated);
        when(userProfileService.getOrCreateCurrent(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "firstName", "Ivan",
                                "lastName", "Petrov",
                                "patronymic", "",
                                "phone", "+79990000000",
                                "bio", "Experienced volunteer",
                                "city", "Moscow"
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VOLUNTEER_ID.toString()))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.fullName").value("Ivan Petrov"))
                .andExpect(jsonPath("$.phone").value("+79990000000"))
                .andExpect(jsonPath("$.city").value("Moscow"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.fullName").value("Ivan Petrov"))
                .andExpect(jsonPath("$.phone").value("+79990000000"));
    }

    @Test
    void volunteerProfileAndHistoryAreBuiltFromProfileAndEvents() throws Exception {
        UserProfile elena = elenaProfile();
        when(userProfileService.getVolunteerProfile(VOLUNTEER_ID)).thenReturn(elena);
        doNothing().when(userProfileService).ensureVolunteerExists(VOLUNTEER_ID);
        when(volunteerRequestHistoryAssembler.completedForVolunteer(VOLUNTEER_ID)).thenReturn(List.of(
                new VolunteerRequestHistoryResponse(
                        SECOND_REQUEST_ID, SECOND_WARD_ID, RequestStatus.COMPLETED,
                        T2, T3, null, T3, null, null, null
                ),
                new VolunteerRequestHistoryResponse(
                        REQUEST_ID, WARD_ID, RequestStatus.COMPLETED,
                        T0, T1, null, T1, null, null, null
                )
        ));

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}", VOLUNTEER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VOLUNTEER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Elena"))
                .andExpect(jsonPath("$.lastName").value("Smirnova"))
                .andExpect(jsonPath("$.fullName").value("Elena Smirnova"))
                .andExpect(jsonPath("$.completedRequestsCount").value(2));

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}/requests/history", VOLUNTEER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(SECOND_REQUEST_ID.toString()))
                .andExpect(jsonPath("$[0].wardId").value(SECOND_WARD_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].requestId").value(REQUEST_ID.toString()));
    }

    @Test
    void wardsCanLeaveRatingsAndVolunteerRatingIsRecalculated() throws Exception {
        when(volunteerRatingService.create(any(), eq(VOLUNTEER_ID), any()))
                .thenReturn(VolunteerRating.create(REQUEST_ID, VOLUNTEER_ID, WARD_ID, 4, T1))
                .thenReturn(VolunteerRating.create(SECOND_REQUEST_ID, VOLUNTEER_ID, SECOND_WARD_ID, 5, T2));

        UserProfile rated = elenaProfile();
        rated.updateRating(new BigDecimal("4.50"), 2, T2);
        when(userProfileService.getVolunteerProfile(VOLUNTEER_ID)).thenReturn(rated);

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 4
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.score").value(4));

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", SECOND_REQUEST_ID,
                                "score", 5
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(SECOND_WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(SECOND_REQUEST_ID.toString()))
                .andExpect(jsonPath("$.score").value(5));

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}", VOLUNTEER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(2));
    }

    @Test
    void wardCannotLeaveDuplicateOrForeignRating() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        when(volunteerRatingService.create(any(), eq(VOLUNTEER_ID), any())).thenAnswer(inv -> {
            int n = calls.getAndIncrement();
            if (n == 0) {
                return VolunteerRating.create(REQUEST_ID, VOLUNTEER_ID, WARD_ID, 5, T0);
            }
            if (n == 1) {
                throw new ConflictException("Already rated");
            }
            throw new ForbiddenException("Not allowed");
        });

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 5
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 4
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", SECOND_REQUEST_ID,
                                "score", 4
                        )))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isForbidden());
    }
}
