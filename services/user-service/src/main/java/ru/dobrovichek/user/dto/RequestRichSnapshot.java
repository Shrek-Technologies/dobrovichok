package ru.dobrovichek.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestRichSnapshot(UUID id, String description) {
}
