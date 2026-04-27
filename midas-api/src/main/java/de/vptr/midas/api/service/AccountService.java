package de.vptr.midas.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PaymentEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for account CRUD business logic. */
@ApplicationScoped
public class AccountService {

    @Inject
    AccountResponseAssembler accountResponseAssembler;

    /** Returns all accounts with default pagination. */
    public List<AccountResponseDto> getAllAccounts() {
        return this.getAllAccounts(0, 20);
    }

    /** Returns all accounts with the given pagination. */
    public List<AccountResponseDto> getAllAccounts(final int page, final int size) {
        final List<AccountEntity> accounts = AccountEntity.findAll()
                .page(page, size)
                .list();
        return this.accountResponseAssembler.toAccountResponseDtos(accounts);
    }

    /** Returns the account with the given id. */
    public Optional<AccountResponseDto> findById(final Long id) {
        return AccountEntity.findByIdOptional(id)
                .map(entity -> this.accountResponseAssembler.toAccountResponseDto((AccountEntity) entity));
    }

    /** Returns the account with the given name. */
    public Optional<AccountResponseDto> findByName(final String name) {
        final AccountEntity entity = AccountEntity.findByName(name);
        return Optional.ofNullable(entity).map(this.accountResponseAssembler::toAccountResponseDto);
    }

    /** Creates a new account. */
    @Transactional
    public AccountResponseDto createAccount(final AccountDto accountDto) {
        this.validateRequiredName(accountDto.name, "creating");

        final AccountEntity account = new AccountEntity();
        account.name = accountDto.name;
        account.created = LocalDateTime.now();
        account.lastEdit = account.created;
        account.persist();

        return this.accountResponseAssembler.toAccountResponseDto(account);
    }

    /** Updates the account with the given id. */
    @Transactional
    public AccountResponseDto updateAccount(final Long id, final AccountDto accountDto) {
        final AccountEntity existingAccount = this.requireExistingAccount(id);
        this.validateRequiredName(accountDto.name, "updating");

        // Complete replacement (PUT semantics)
        existingAccount.name = accountDto.name;
        existingAccount.lastEdit = LocalDateTime.now();

        existingAccount.persist();

        return this.accountResponseAssembler.toAccountResponseDto(existingAccount);
    }

    /** Partially updates the account with the given id. */
    @Transactional
    public AccountResponseDto patchAccount(final Long id, final AccountDto accountDto) {
        final AccountEntity existingAccount = this.requireExistingAccount(id);

        // Partial update (PATCH semantics) - only update provided fields
        if (accountDto.name != null) {
            existingAccount.name = accountDto.name;
        }

        existingAccount.lastEdit = LocalDateTime.now();
        existingAccount.persist();
        return this.accountResponseAssembler.toAccountResponseDto(existingAccount);
    }

    /** Deletes the account with the given id. */
    @Transactional
    public boolean deleteAccount(final Long id) {
        final var account = AccountEntity.findById(id);
        if (account == null) {
            return false;
        }

        // Check if account has any payments
        final long paymentCount = PaymentEntity.count(
                "sourceAccount.id = ?1 OR targetAccount.id = ?1", id);
        if (paymentCount > 0) {
            throw new WebApplicationException("Cannot delete account with existing payments", Response.Status.CONFLICT);
        }

        return AccountEntity.deleteById(id);
    }

    /** Returns all accounts matching the given query. */
    public List<AccountResponseDto> searchAccounts(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllAccounts();
        }

        final List<AccountEntity> accounts = SearchQueryUtil.applyDefaultSearchLimit(AccountEntity.find(
                "SELECT DISTINCT a FROM AccountEntity a "
                        + "LEFT JOIN a.userAccountMetas uam "
                        + "LEFT JOIN uam.user u "
                        + "WHERE LOWER(a.name) LIKE ?1 ESCAPE '\\' OR LOWER(u.username) LIKE ?1 ESCAPE '\\'",
                searchTerm)).list();

        return this.accountResponseAssembler.toAccountResponseDtos(accounts);
    }

    private AccountEntity requireExistingAccount(final Long id) {
        final AccountEntity existingAccount = AccountEntity.findById(id);
        if (existingAccount == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }
        return existingAccount;
    }

    private void validateRequiredName(final String name, final String action) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name is required for " + action + " an account");
        }
    }
}
