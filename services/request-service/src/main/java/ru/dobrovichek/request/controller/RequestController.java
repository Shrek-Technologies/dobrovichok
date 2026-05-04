package ru.dobrovichek.request.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.request.service.RequestCommandService;
import ru.dobrovichek.request.util.RequestMapper;
import ru.dobrovichek.request.service.RequestQueryService;
import ru.dobrovichek.request.dto.CreateRequestRequest;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.dto.RequestResponse;
import ru.dobrovichek.request.dto.RequestSummaryResponse;
import ru.dobrovichek.request.entity.HelpRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestCommandService commandService;
    private final RequestQueryService queryService;
    private final RequestMapper mapper;

    public RequestController(
            RequestCommandService commandService,
            RequestQueryService queryService,
            RequestMapper mapper
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @Operation(summary = "Создать заявку", description = "Новая заявка на помощь от подопечного")
    @PostMapping
    public ResponseEntity<RequestResponse> create(
            @Valid @RequestBody CreateRequestRequest request,
            CurrentUser currentUser
    ) {
        HelpRequest created = commandService.create(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created, true));
    }

    @Operation(summary = "Активная заявка", description = "Текущая незавершённая заявка пользователя")
    @GetMapping("/active")
    public ResponseEntity<RequestResponse> getActive(CurrentUser currentUser) {
        Optional<HelpRequest> active = queryService.findActiveRequest(currentUser);
        if (active.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HelpRequest request = active.get();
        return ResponseEntity.ok(
                mapper.toResponse(request, queryService.canViewContact(request, currentUser))
        );
    }

    @Operation(summary = "Заявка по id", description = "Полная информация при наличии прав доступа")
    @GetMapping("/{requestId}")
    public RequestResponse getById(
            @PathVariable UUID requestId,
            CurrentUser currentUser
    ) {
        HelpRequest request = queryService.getById(requestId);
        queryService.assertCanRead(request, currentUser);
        return mapper.toResponse(request, queryService.canViewContact(request, currentUser));
    }

    @Operation(summary = "Заявки рядом", description = "Список заявок в радиусе от координат (для карты)")
    @GetMapping("/nearby")
    public List<RequestSummaryResponse> findNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "1.0") @DecimalMin("0.1") @DecimalMax("50.0") double radiusKm,
            @RequestParam(required = false) Set<RequestStatus> statuses,
            CurrentUser currentUser
    ) {
        return queryService.findNearby(currentUser, latitude, longitude, radiusKm, statuses)
                .stream()
                .map(candidate -> mapper.toSummaryResponse(candidate.request(), candidate.distanceKm()))
                .toList();
    }

    @Operation(summary = "Принять заявку", description = "Волонтёр берёт заявку в работу")
    @PostMapping("/{requestId}/accept")
    public RequestResponse accept(
            @PathVariable UUID requestId,
            CurrentUser currentUser
    ) {
        HelpRequest updated = commandService.accept(requestId, currentUser);
        return mapper.toResponse(updated, true);
    }

    @Operation(summary = "Отменить заявку", description = "Отмена заявки подопечным или по правилам доступа")
    @PostMapping("/{requestId}/cancel")
    public RequestResponse cancel(
            @PathVariable UUID requestId,
            CurrentUser currentUser
    ) {
        HelpRequest updated = commandService.cancel(requestId, currentUser);
        return mapper.toResponse(updated, true);
    }

    @Operation(summary = "Завершить заявку", description = "Отметить выполнение помощи")
    @PostMapping("/{requestId}/complete")
    public RequestResponse complete(
            @PathVariable UUID requestId,
            CurrentUser currentUser
    ) {
        HelpRequest updated = commandService.complete(requestId, currentUser);
        return mapper.toResponse(updated, true);
    }

    @Operation(summary = "Отказ волонтёра", description = "Волонтёр снимается с принятой заявки")
    @PostMapping("/{requestId}/abandon-volunteer")
    public RequestResponse abandonVolunteer(
            @PathVariable UUID requestId,
            CurrentUser currentUser
    ) {
        HelpRequest updated = commandService.abandonByVolunteer(requestId, currentUser);
        return mapper.toResponse(updated, queryService.canViewContact(updated, currentUser));
    }
}
