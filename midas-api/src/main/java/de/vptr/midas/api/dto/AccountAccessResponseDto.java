package de.vptr.midas.api.dto;

/** Response DTO indicating whether the current user has access to an account. */
public record AccountAccessResponseDto(boolean hasAccess) {
}
