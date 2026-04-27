package de.vptr.midas.api.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for account operations (POST, PUT, PATCH).
 * 
 * - POST: name required (validated by service)
 * - PUT: name required (validated by service)
 * - PATCH: name optional (allows null)
 */
public class AccountDto {

    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters when provided")
    public String name;

}
