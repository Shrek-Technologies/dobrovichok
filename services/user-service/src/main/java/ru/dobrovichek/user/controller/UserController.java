package ru.dobrovichek.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import ru.dobrovichek.user.util.UserProfileMapper;
import ru.dobrovichek.user.service.UserProfileService;
import ru.dobrovichek.user.service.VolunteerRatingService;
import ru.dobrovichek.user.util.VolunteerRequestHistoryAssembler;
import ru.dobrovichek.user.dto.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserProfileService userProfileService;
    private final VolunteerRequestHistoryAssembler volunteerRequestHistoryAssembler;
    private final VolunteerRatingService volunteerRatingService;
    private final UserProfileMapper mapper;

    public UserController(
            UserProfileService userProfileService,
            VolunteerRequestHistoryAssembler volunteerRequestHistoryAssembler,
            VolunteerRatingService volunteerRatingService,
            UserProfileMapper mapper
    ) {
        this.userProfileService = userProfileService;
        this.volunteerRequestHistoryAssembler = volunteerRequestHistoryAssembler;
        this.volunteerRatingService = volunteerRatingService;
        this.mapper = mapper;
    }

    @Operation(summary = "Мой профиль", description = "Данные текущего пользователя")
    @GetMapping("/users/me")
    public UserProfileResponse getMyProfile(CurrentUser currentUser) {
        return mapper.toUserProfileResponse(userProfileService.getOrCreateCurrent(currentUser));
    }

    @Operation(summary = "Обновить профиль", description = "Имя, контакты и прочие поля профиля")
    @PutMapping("/users/me")
    public UserProfileResponse updateMyProfile(
            CurrentUser currentUser,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return mapper.toUserProfileResponse(userProfileService.upsertCurrent(currentUser, request));
    }

    @Operation(summary = "Регистрация устройства", description = "Сохранение FCM-токена для push-уведомлений")
    @PutMapping("/users/me/device")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(
            CurrentUser currentUser,
            @RequestBody RegisterDeviceRequest request
    ) {
        userProfileService.registerDevice(currentUser, request);
    }

    @Operation(summary = "Профиль волонтёра", description = "Публичные данные и рейтинг")
    @GetMapping("/volunteers/{volunteerId}")
    public VolunteerProfileResponse getVolunteerProfile(@PathVariable UUID volunteerId) {
        return mapper.toVolunteerProfileResponse(userProfileService.getVolunteerProfile(volunteerId));
    }

    @Operation(summary = "История заявок волонтёра", description = "Завершённые заявки по волонтёру")
    @GetMapping("/volunteers/{volunteerId}/requests/history")
    public List<VolunteerRequestHistoryResponse> getVolunteerHistory(@PathVariable UUID volunteerId) {
        userProfileService.ensureVolunteerExists(volunteerId);
        return volunteerRequestHistoryAssembler.completedForVolunteer(volunteerId);
    }

    @Operation(summary = "Оценка волонтёра", description = "Выставить оценку после выполненной заявки")
    @PostMapping("/volunteers/{volunteerId}/ratings")
    public VolunteerRatingResponse createVolunteerRating(
            @PathVariable UUID volunteerId,
            CurrentUser currentUser,
            @Valid @RequestBody CreateVolunteerRatingRequest request
    ) {
        return mapper.toVolunteerRatingResponse(volunteerRatingService.create(currentUser, volunteerId, request));
    }
}
