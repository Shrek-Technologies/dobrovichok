package ru.dobrovichek.request;

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
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.jwt.JwtProperties;
import ru.dobrovichek.jwt.JwtTokenIssuer;
import ru.dobrovichek.request.config.SecurityConfiguration;
import ru.dobrovichek.request.controller.RequestController;
import ru.dobrovichek.request.controller.RestExceptionHandler;
import ru.dobrovichek.request.controller.WebMvcConfiguration;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.entity.HelpRequest;
import ru.dobrovichek.request.exception.ConflictException;
import ru.dobrovichek.request.exception.ForbiddenException;
import ru.dobrovichek.request.exception.RequestNotFoundException;
import ru.dobrovichek.request.service.RequestCommandService;
import ru.dobrovichek.request.service.RequestQueryService;
import ru.dobrovichek.request.util.CurrentUserArgumentResolver;
import ru.dobrovichek.request.util.RequestMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestController.class)
@Import({
        RestExceptionHandler.class,
        SecurityConfiguration.class,
        WebMvcConfiguration.class,
        CurrentUserArgumentResolver.class,
        RequestMapper.class
})
@TestPropertySource(properties = {
        "dobrovichek.jwt.secret=test-test-test-test-test-test-test-test-32bytes!",
        "dobrovichek.jwt.issuer=dobrovichek"
})
class RequestControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T00:01:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T00:02:00Z");

    private static final UUID WARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VOLUNTEER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_VOLUNTEER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private RequestCommandService commandService;

    @MockBean
    private RequestQueryService queryService;

    private String bearer(UUID userId, UserRole role) {
        return "Bearer " + new JwtTokenIssuer(jwtProperties).createAccessToken(userId, role);
    }

    private HelpRequest newCreatedRequest() {
        return HelpRequest.create(
                WARD_ID,
                "Need groceries",
                "+79990000000",
                "Ольга",
                "Козлова",
                "Сергеевна",
                new GeoPoint(59.9343, 30.3351),
                T0
        );
    }

    private Map<String, Object> createPayload() {
        return Map.of(
                "description", "Need groceries",
                "contactPhone", "+79990000000",
                "wardFirstName", "Ольга",
                "wardLastName", "Козлова",
                "wardPatronymic", "Сергеевна",
                "location", Map.of(
                        "latitude", 59.9343,
                        "longitude", 30.3351
                )
        );
    }

    @Test
    void wardCanCreateRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wardId").value(WARD_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.contactPhone").value("+79990000000"));
    }

    @Test
    void volunteerCannotCreateRequest() throws Exception {
        when(commandService.create(any(CurrentUser.class), any()))
                .thenThrow(new ForbiddenException("Only wards can create requests"));

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void volunteerCanAcceptRequestAndSeeContact() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(commandService.accept(eq(created.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            created.accept(VOLUNTEER_ID, T1);
            return created;
        });

        String response = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/v1/requests/{requestId}/accept", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.volunteerId").value(VOLUNTEER_ID.toString()))
                .andExpect(jsonPath("$.contactPhone").value("+79990000000"));
    }

    @Test
    void assignedVolunteerCanCompleteRequest() throws Exception {
        HelpRequest accepted = newCreatedRequest();
        accepted.accept(VOLUNTEER_ID, T1);
        when(commandService.complete(eq(accepted.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            accepted.complete(T2);
            return accepted;
        });

        mockMvc.perform(post("/api/v1/requests/{requestId}/complete", accepted.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void otherVolunteerCannotReadAcceptedRequest() throws Exception {
        HelpRequest accepted = newCreatedRequest();
        accepted.accept(VOLUNTEER_ID, T1);
        when(queryService.getById(accepted.getId())).thenReturn(accepted);
        doThrow(new ForbiddenException("You do not have access to this request"))
                .when(queryService).assertCanRead(eq(accepted), any(CurrentUser.class));

        mockMvc.perform(get("/api/v1/requests/{requestId}", accepted.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wardActiveReturnsCreatedRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(queryService.findActiveRequest(any(CurrentUser.class))).thenReturn(Optional.of(created));
        when(queryService.canViewContact(eq(created), any(CurrentUser.class))).thenReturn(true);

        String createResponse = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String requestId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/requests/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void volunteerActiveReturnsAcceptedRequest() throws Exception {
        HelpRequest accepted = newCreatedRequest();
        accepted.accept(VOLUNTEER_ID, T1);
        when(queryService.findActiveRequest(any(CurrentUser.class))).thenReturn(Optional.of(accepted));
        when(queryService.canViewContact(eq(accepted), any(CurrentUser.class))).thenReturn(true);

        mockMvc.perform(get("/api/v1/requests/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accepted.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void volunteerActiveReturns404WhenNothingAccepted() throws Exception {
        when(queryService.findActiveRequest(any(CurrentUser.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/requests/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void volunteerCanFindNearbyCreatedRequests() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(queryService.findNearby(any(CurrentUser.class), eq(59.9343), eq(30.3351), eq(1.0d), isNull()))
                .thenReturn(List.of(new RequestQueryService.NearbyRequestCandidate(created, 0.42)));

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/requests/nearby")
                        .param("latitude", "59.9343")
                        .param("longitude", "30.3351")
                        .param("radiusKm", "1.0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wardFirstName").value("Ольга"))
                .andExpect(jsonPath("$[0].description").value("Need groceries"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.42));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        UUID missing = UUID.randomUUID();
        when(queryService.getById(missing)).thenThrow(new RequestNotFoundException(missing));

        mockMvc.perform(get("/api/v1/requests/{requestId}", missing)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void wardCannotAcceptRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(commandService.accept(eq(created.getId()), any(CurrentUser.class)))
                .thenThrow(new ForbiddenException("Only volunteers can accept requests"));

        String createResponse = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/requests/{requestId}/accept", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wardCanCancelCreatedRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(commandService.cancel(eq(created.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            created.cancel(T1);
            return created;
        });

        String createResponse = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String requestId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/requests/{requestId}/cancel", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void volunteerCannotCancelRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(commandService.cancel(eq(created.getId()), any(CurrentUser.class)))
                .thenThrow(new ForbiddenException("Only wards can cancel requests"));

        String createResponse = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String requestId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/requests/{requestId}/cancel", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotAcceptAlreadyAcceptedRequest() throws Exception {
        HelpRequest created = newCreatedRequest();
        when(commandService.create(any(CurrentUser.class), any())).thenReturn(created);
        when(commandService.accept(eq(created.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            CurrentUser user = inv.getArgument(1);
            if (user.userId().equals(VOLUNTEER_ID)) {
                created.accept(VOLUNTEER_ID, T1);
                return created;
            }
            throw new ConflictException("Only created requests can be accepted");
        });

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/requests/{requestId}/accept", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/requests/{requestId}/accept", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isConflict());
    }

    @Test
    void wardCanCompleteAcceptedRequest() throws Exception {
        HelpRequest accepted = newCreatedRequest();
        accepted.accept(VOLUNTEER_ID, T1);
        when(commandService.complete(eq(accepted.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            accepted.complete(T2);
            return accepted;
        });

        mockMvc.perform(post("/api/v1/requests/{requestId}/complete", accepted.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void volunteerCanAbandonAcceptedRequest() throws Exception {
        HelpRequest accepted = newCreatedRequest();
        accepted.accept(VOLUNTEER_ID, T1);
        when(commandService.abandonByVolunteer(eq(accepted.getId()), any(CurrentUser.class))).thenAnswer(inv -> {
            accepted.abandonByVolunteer(T2);
            return accepted;
        });
        when(queryService.canViewContact(any(HelpRequest.class), any(CurrentUser.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/requests/{requestId}/abandon-volunteer", accepted.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void nearbyRejectsRadiusBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/v1/requests/nearby")
                        .param("latitude", "59.9343")
                        .param("longitude", "30.3351")
                        .param("radiusKm", "0.05")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyRejectsRadiusAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/requests/nearby")
                        .param("latitude", "59.9343")
                        .param("longitude", "30.3351")
                        .param("radiusKm", "60")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequestValidatesPayload() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isBadRequest());
    }
}
