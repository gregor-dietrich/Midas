package de.vptr.midas.api.dto;

/** Request DTO for updating user-account meta properties. */
public class UserAccountMetaDto {

    /** Whether this account represents a negative (debt/liability) balance for the user. */
    public boolean isNegative;

    /** Whether the user can manage (add, edit, delete) payments for this account. */
    public boolean canManage;
}
