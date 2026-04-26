package ru.dobrovichek.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.security.ServiceHeaders;
import ru.dobrovichek.user.application.VolunteerHistoryProjector;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRatingJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.UserProfileJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRequestHistoryJpaRepository;
import ru.dobrovichek.user.infrastructure.request.RequestRichSnapshot;
import ru.dobrovichek.user.infrastructure.request.RequestServiceClient;
import ru.dobrovichek.user.infrastructure.request.RequestSnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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
    private VolunteerHistoryProjector volunteerHistoryProjector;

    @Autowired
    private UserProfileJpaRepository userProfileRepository;

    @Autowired
    private VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository;

    @Autowired
    private VolunteerRatingJpaRepository volunteerRatingRepository;

    @MockBean
    private RequestServiceClient requestServiceClient;

    @BeforeEach
    void stubRequestServiceClient() {
        Instant accepted1 = Instant.parse("2026-04-23T12:00:00Z");
        Instant completed1 = Instant.parse("2026-04-23T13:00:00Z");
        Instant accepted2 = Instant.parse("2026-04-23T14:00:00Z");
        Instant completed2 = Instant.parse("2026-04-23T15:00:00Z");
        when(requestServiceClient.getRequestAsWard(eq(REQUEST_ID), eq(WARD_ID)))
                .thenReturn(Optional.of(new RequestSnapshot(
                        REQUEST_ID,
                        WARD_ID,
                        VOLUNTEER_ID,
                        RequestStatus.COMPLETED,
                        accepted1,
                        completed1
                )));
        when(requestServiceClient.getRequestAsWard(eq(SECOND_REQUEST_ID), eq(SECOND_WARD_ID)))
                .thenReturn(Optional.of(new RequestSnapshot(
                        SECOND_REQUEST_ID,
                        SECOND_WARD_ID,
                        VOLUNTEER_ID,
                        RequestStatus.COMPLETED,
                        accepted2,
                        completed2
                )));
        when(requestServiceClient.getRequestAsWard(eq(SECOND_REQUEST_ID), eq(WARD_ID)))
                .thenReturn(Optional.of(new RequestSnapshot(
                        SECOND_REQUEST_ID,
                        SECOND_WARD_ID,
                        VOLUNTEER_ID,
                        RequestStatus.COMPLETED,
                        accepted2,
                        completed2
                )));

        when(requestServiceClient.getRequestAsVolunteer(eq(REQUEST_ID), eq(VOLUNTEER_ID)))
                .thenReturn(Optional.of(new RequestRichSnapshot(
                        REQUEST_ID,
                        "Категория: Доставка\nСрочность: Сейчас\nАдрес: ул. Пример, 1\n"
                )));
        when(requestServiceClient.getRequestAsVolunteer(eq(SECOND_REQUEST_ID), eq(VOLUNTEER_ID)))
                .thenReturn(Optional.of(new RequestRichSnapshot(
                        SECOND_REQUEST_ID,
                        "Категория: Настройка техники\nСрочность: Сейчас\nАдрес: пр-т Тестовый, 2\n"
                )));
    }

    @AfterEach
    void tearDown() {
        volunteerRatingRepository.deleteAll();
        volunteerRequestHistoryRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void getMyProfileCreatesShellFromHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(WARD_ID.toString()))
                .andExpect(jsonPath("$.role").value("WARD"))
                .andExpect(jsonPath("$.firstName").value(""))
                .andExpect(jsonPath("$.lastName").value(""))
                .andExpect(jsonPath("$.fullName").value(""));
    }

    @Test
    void putMyProfileUpdatesCurrentUserProfile() throws Exception {
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
                        .header(ServiceHeaders.USER_ID, VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.VOLUNTEER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VOLUNTEER_ID.toString()))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.fullName").value("Ivan Petrov"))
                .andExpect(jsonPath("$.phone").value("+79990000000"))
                .andExpect(jsonPath("$.city").value("Moscow"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(ServiceHeaders.USER_ID, VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.VOLUNTEER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.fullName").value("Ivan Petrov"))
                .andExpect(jsonPath("$.phone").value("+79990000000"));
    }

    @Test
    void volunteerProfileAndHistoryAreBuiltFromProfileAndEvents() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "firstName", "Elena",
                                "lastName", "Smirnova",
                                "patronymic", "",
                                "phone", "+79991111111",
                                "bio", "Volunteer driver",
                                "city", "Saint Petersburg"
                        )))
                        .header(ServiceHeaders.USER_ID, VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.VOLUNTEER.name()))
                .andExpect(status().isOk());

        projectCompletedRequests();

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}", VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VOLUNTEER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Elena"))
                .andExpect(jsonPath("$.lastName").value("Smirnova"))
                .andExpect(jsonPath("$.fullName").value("Elena Smirnova"))
                .andExpect(jsonPath("$.completedRequestsCount").value(2));

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}/requests/history", VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(SECOND_REQUEST_ID.toString()))
                .andExpect(jsonPath("$[0].wardId").value(SECOND_WARD_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].requestId").value(REQUEST_ID.toString()));
    }

    @Test
    void wardsCanLeaveRatingsAndVolunteerRatingIsRecalculated() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "firstName", "Elena",
                                "lastName", "Smirnova",
                                "patronymic", "",
                                "phone", "+79991111111",
                                "bio", "Volunteer driver",
                                "city", "Saint Petersburg"
                        )))
                        .header(ServiceHeaders.USER_ID, VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.VOLUNTEER.name()))
                .andExpect(status().isOk());

        projectCompletedRequests();

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 4
                        )))
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.score").value(4));

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", SECOND_REQUEST_ID,
                                "score", 5
                        )))
                        .header(ServiceHeaders.USER_ID, SECOND_WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(SECOND_REQUEST_ID.toString()))
                .andExpect(jsonPath("$.score").value(5));

        mockMvc.perform(get("/api/v1/volunteers/{volunteerId}", VOLUNTEER_ID)
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(2));
    }

    @Test
    void wardCannotLeaveDuplicateOrForeignRating() throws Exception {
        projectCompletedRequests();

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 5
                        )))
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", REQUEST_ID,
                                "score", 4
                        )))
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/volunteers/{volunteerId}/ratings", VOLUNTEER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", SECOND_REQUEST_ID,
                                "score", 4
                        )))
                        .header(ServiceHeaders.USER_ID, WARD_ID)
                        .header(ServiceHeaders.USER_ROLE, UserRole.WARD.name()))
                .andExpect(status().isForbidden());
    }

    private void projectCompletedRequests() {
        volunteerHistoryProjector.project(new RequestStatusChangedEvent(
                REQUEST_ID,
                WARD_ID,
                VOLUNTEER_ID,
                RequestStatus.ACCEPTED,
                Instant.parse("2026-04-23T12:00:00Z")
        ));
        volunteerHistoryProjector.project(new RequestStatusChangedEvent(
                REQUEST_ID,
                WARD_ID,
                VOLUNTEER_ID,
                RequestStatus.COMPLETED,
                Instant.parse("2026-04-23T13:00:00Z")
        ));
        volunteerHistoryProjector.project(new RequestStatusChangedEvent(
                SECOND_REQUEST_ID,
                SECOND_WARD_ID,
                VOLUNTEER_ID,
                RequestStatus.ACCEPTED,
                Instant.parse("2026-04-23T14:00:00Z")
        ));
        volunteerHistoryProjector.project(new RequestStatusChangedEvent(
                SECOND_REQUEST_ID,
                SECOND_WARD_ID,
                VOLUNTEER_ID,
                RequestStatus.COMPLETED,
                Instant.parse("2026-04-23T15:00:00Z")
        ));
    }
}
