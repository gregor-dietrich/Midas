# Midas Code Review TODOs

Remaining items are either deferred (large refactors), rejected (verified false
positives), or partially complete with a scoped remainder. All other reviewed
findings have been fixed and removed from this file.

## Performance Issues

- [ ] **Performance: Inefficient Client-Side Filtering** (DEFERRED): Several GUI
      views (e.g., `AccountView.java`) load the entire dataset into the Vaadin
      server's memory and use `ListDataProvider` for filtering/sorting. As the
      database grows, this will cause memory pressure and slow UI response times.
      Server-side pagination and filtering should be implemented. Deferred: this is
      a cross-cutting architectural change spanning many views and the API, out of
      scope for a minimal-change pass.

## Logical Bugs & Architectural Flaws

- [ ] **Robustness: NullAway Strict Audit & Enforcement** (PARTIALLY DONE):
  - Done: audited every `@SuppressWarnings("NullAway")` in non-test
    `src/main/java`; removed all unjustified suppressions (services, dialogs,
    panels, and DTOs such as `AbstractTitleContentDto`, `AbstractPostDto`,
    `AbstractUserRankDto`, `AbstractPostCommentDto`, `AccountDto`, etc.) by
    fixing the underlying nullability with `@Nullable` annotations and
    `Objects.requireNonNull` guards.
  - Remaining (deferred): the Jackson response DTOs
    (`PostResponseDto`, `UserResponseDto`, `AccountResponseDto`,
    `UserRankResponseDto`, `PageResponseDto`, `PaymentResponseDto`,
    `PostCommentResponseDto`, `PostCategoryResponseDto`, `UserGroupResponseDto`,
    `PostDto`, `PostCommentDto`) retain `@SuppressWarnings("NullAway")` on their
    no-arg Jackson constructors. Removing these requires marking every field
    `@Nullable` and rippling the change through all consumers — a larger API
    refactor.
  - Blocked on the above: the Checkstyle ban on `@SuppressWarnings("NullAway")`
    is intentionally NOT added yet, since justified suppressions still remain in
    non-test source. Add the (test-excluded) Checkstyle rule only once the
    response-DTO refactor clears the remaining suppressions.
