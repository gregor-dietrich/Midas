# Midas Code Review TODOs

Remaining items are either deferred (large refactors), rejected (verified false
positives), or partially complete with a scoped remainder. All other reviewed
findings have been fixed and removed from this file.

## Performance Issues

- [ ] **Performance: Inefficient Client-Side Filtering** (RE-SCOPED — payments
      path pending): Some GUI views load unbounded datasets into Vaadin server
      memory and filter/aggregate client-side. Re-scoped after confirming the
      `payment` table reaches thousands–tens of thousands of rows while all other
      entities stay small:
  - Pending (high value): the **payment path**. `AccountView` loads all payments
    for all of the user's accounts and sums balances client-side
    (`recalculateBalances()`); `AccountPaymentView` loads all payments for one
    account and filters/sums client-side. Fix = server-side filtered+paginated
    payment listing (reuse the lazy `DataProvider` pattern from
    `AdminUserView.setupLazyDataProvider()` + `PaginationParamUtil`) **plus** a
    dedicated server-side balance/aggregate endpoint (a balance is a sum over a
    date range and cannot be derived from one Vaadin page).
  - Optional follow-up (consistency only): `PostView` and any remaining
    non-admin client-filtered views adopt the same lazy pattern. Low runtime
    value (these entities stay small) but supports the example-project goal.
  - Permanently out of scope: a generic pagination framework across all views —
    reuse the existing per-view lazy pattern instead.

## Logical Bugs & Architectural Flaws

- [x] **Robustness: NullAway Strict Audit & Enforcement** (DONE):
  - Audited every `@SuppressWarnings("NullAway")` in non-test `src/main/java`;
    removed all unjustified suppressions (services, dialogs, panels, and DTOs
    such as `AbstractTitleContentDto`, `AbstractPostDto`, `AbstractUserRankDto`,
    `AbstractPostCommentDto`, `AccountDto`, etc.) by fixing the underlying
    nullability with `@Nullable` annotations and `Objects.requireNonNull`
    guards.
  - Accepted scoped exception (not a deferral): the 11 Jackson response DTOs
    (`PostResponseDto`, `UserResponseDto`, `AccountResponseDto`,
    `UserRankResponseDto`, `PageResponseDto`, `PaymentResponseDto`,
    `PostCommentResponseDto`, `PostCategoryResponseDto`, `UserGroupResponseDto`,
    `PostDto`, `PostCommentDto`) keep `@SuppressWarnings("NullAway")` on their
    no-arg Jackson constructors. This is the correct semantics for Jackson's
    two-phase deserialization (construct empty → setters populate); marking
    every field `@Nullable` would be pessimistic across hundreds of
    reliably-populated read sites for no real safety gain.
  - Checkstyle ban ADDED (scoped): `checkstyle.xml` now bans
    `@SuppressWarnings("NullAway")` via a `RegexpSinglelineJava` module with
    `id="SuppressWarningsNullAway"`. Each module's `checkstyle-suppressions.xml`
    exempts (a) the `midas-gui` `dto` package and (b) test sources. New,
    unjustified suppressions in services/views/dialogs/panels now fail the
    build.
