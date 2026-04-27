package de.vptr.midas.api.exception;

/** Shared API error payload contract. */
public record ApiErrorResponse(int status, String error, String message) {
}
