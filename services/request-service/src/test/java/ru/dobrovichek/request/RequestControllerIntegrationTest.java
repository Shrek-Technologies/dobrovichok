package ru.dobrovichek.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.jwt.JwtProperties;
import ru.dobrovichek.jwt.JwtTokenIssuer;
import ru.dobrovichek.request.repository.HelpRequestRepository;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestControllerIntegrationTest {

    private static final UUID WARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VOLUNTEER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_VOLUNTEER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HelpRequestRepository repository;

    @Autowired
    private JwtProperties jwtProperties;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private String bearer(UUID userId, UserRole role) {
        return "Bearer " + new JwtTokenIssuer(jwtProperties).createAccessToken(userId, role);
    }

    @Test
    void wardCanCreateRequest() throws Exception {
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
        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void volunteerCanAcceptRequestAndSeeContact() throws Exception {
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
        String requestId = createAndAcceptRequest();

        mockMvc.perform(post("/api/v1/requests/{requestId}/complete", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void otherVolunteerCannotReadAcceptedRequest() throws Exception {
        String requestId = createAndAcceptRequest();

        mockMvc.perform(get("/api/v1/requests/{requestId}", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wardActiveReturnsCreatedRequest() throws Exception {
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
        String requestId = createAndAcceptRequest();

        mockMvc.perform(get("/api/v1/requests/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void volunteerActiveReturns404WhenNothingAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/requests/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void volunteerCanFindNearbyCreatedRequests() throws Exception {
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
                .andExpect(jsonPath("$[0].distanceKm").isNumber());
    }

    private String createAndAcceptRequest() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createPayload()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(WARD_ID, UserRole.WARD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/requests/{requestId}/accept", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(VOLUNTEER_ID, UserRole.VOLUNTEER)))
                .andExpect(status().isOk());

        return requestId;
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
}
