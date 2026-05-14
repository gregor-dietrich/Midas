package de.vptr.midas.api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AccountResponseAssemblerTest {

    private final AccountResponseAssembler assembler = new AccountResponseAssembler();

    @Test
    void toAccountResponseDtos_shouldReturnEmptyList_whenInputIsEmpty() {
        assertTrue(this.assembler.toAccountResponseDtos(List.of()).isEmpty());
    }
}
