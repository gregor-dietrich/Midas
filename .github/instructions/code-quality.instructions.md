---
description: "Code quality, maintainability, and security standards. Applies to all developers, reviewers, and architects. Focus: clean code, DRY principle, code duplication elimination, security, and performance."
applyTo: "**"
---

# Code Quality, Maintainability & Security Standards — Midas

This document defines expectations for all contributors: **developers** (when writing code), **reviewers** (when reviewing PRs), and **architects** (when designing features).

---

## Principles

### 1. Clean, Maintainable Code is Non-Negotiable

- **Clarity over cleverness**: Code should be obvious to the next developer (or your future self).
- **Self-documenting code**: Good naming, structure, and logic flow reduce need for comments.
- **Single Responsibility**: Each class/method should do one thing well.
- **No large code blocks**: Methods >30 lines are a smell. Extract helper methods or refactor.
- **Consistent style**: Follow existing patterns in the codebase. Consistency matters more than personal preference.

### 2. Eliminate Code Duplication (DRY)

- **Extract common logic**: Copy-paste code is never acceptable. Extract to shared methods/classes immediately.
- **Shared utilities**: If 2+ places do the same thing, centralize it (e.g., validation logic, error handling, transformations).
- **Base classes & traits**: Use inheritance or composition for repeated patterns (e.g., `AbstractAsyncDataView` for GUI views).
- **Constants**: Don't scatter magic values. Use named constants or configuration.
- **Duplication debt = technical debt**: Every duplicate is a bug waiting to happen when one copy is fixed but others aren't.

### 3. Security is Default

- **Validate all input**: Never trust external input. Validate early and often.
  - REST request bodies: validate via `@Valid` annotations or explicit checks
  - Query parameters: sanitize and validate
  - User-provided data: escape/encode before use in output (HTML, SQL, etc.)
- **Least privilege**: Use `@PermitAll` only where absolutely public. Default to `@RolesAllowed` or `@DenyAll`.
- **No secrets in code**: Never hardcode passwords, API keys, or tokens. Use environment variables or secure vaults.
- **HTTPS only**: Verify TLS/SSL in production configurations.
- **Sanitization**: Use framework provided sanitizers (e.g., `html_escape` in templates, parameterized SQL queries).
- **Authentication & Authorization**: Centralize checks. Don't scatter security logic throughout code.
- **Logging**: Don't log sensitive data (passwords, tokens, PII). Log security events (login failures, unauthorized attempts).

### 4. Performance Matters

- **Lazy loading**: Don't load unnecessary data upfront. Use pagination, filters, and limits.
- **N+1 queries**: Avoid fetching related data in loops. Use JPA `@Fetch(FetchMode.JOIN)` or explicit queries.
- **Caching**: Cache frequently accessed, rarely changing data (configuration, user roles, etc.).
- **Batch operations**: When updating multiple records, batch them to reduce database round-trips.
- **Indexes**: Ensure database columns used in WHERE/JOIN clauses are indexed.
- **Connection pooling**: Verify database connection pool is configured with reasonable limits.
- **Resource cleanup**: Close streams, connections, and resources explicitly (or use try-with-resources).
- **Profiling**: Before optimizing, profile. Don't guess where bottlenecks are.
- **Early exits**: Return early from methods to avoid unnecessary computation.

---

## For Developers

### When Writing Code

1. **Check if code exists**: Before writing, search the codebase for similar logic. Reuse or extend existing solutions.
2. **Follow established patterns**: Use the same architectural style, naming conventions, and error handling as existing code.
3. **Test as you code**: Write tests alongside code (TDD is preferred but at minimum test before submitting).
4. **Think about edge cases**: Handle null, empty, negative, and boundary conditions.
5. **Comments are for why, not what**: Good code should explain what it does. Comments explain why you chose this approach (or why a seemingly obvious fix won't work).
6. **Avoid large PRs**: Smaller, focused changes are easier to review and less likely to introduce bugs.
7. **Lint & test locally**: Don't rely on CI to catch style violations or test failures. Run `make test` and `make check` before pushing.

### Code Organization

- **Packages**: Group related functionality. Avoid flat package structures.
- **Classes**: One responsibility per class. Move utils to utilities packages.
- **Methods**: Extract complex logic into well-named helper methods (even if called once—clarity matters).
- **Imports**: Keep imports organized and minimal. Remove unused imports. Do NOT use wildcard imports. Do NOT use FQCNs (fully qualified class names) - always import properly.

### Error Handling

- **Explicit exceptions**: Catch specific exceptions, not `Exception`.
- **Centralized handlers**: Use existing error handlers (e.g., `ErrorHandler` in GUI, global exception mappers in API).
- **Meaningful messages**: Error messages should help the user/developer understand the problem and fix it.
- **Fail fast**: Don't silently swallow errors. Log or rethrow appropriately.

---

## For Reviewers

Your job is to **catch problems before they hit production** and **maintain code quality standards**.

### What to Check

#### 1. Code Duplication

- Search for similar logic in other files. Flag for extraction if found.
- Look for copy-pasted code blocks (common in UI forms, API endpoints).
- Suggest using existing utilities or base classes.

#### 2. Security Issues

- Check for hardcoded secrets (passwords, API keys, tokens).
- Verify input validation on user-provided data (query params, request bodies, file uploads).
- Check for SQL injection risks (use parameterized queries, never concatenate strings).
- Verify authentication/authorization decorators (`@PermitAll`, `@RolesAllowed`, `@DenyAll`).
- Look for sensitive data in logs.

#### 3. Performance Red Flags

- N+1 queries (loop making database calls).
- Unnecessary data loads (loading entire collections when paginating).
- Missing indexes on frequently filtered columns.
- Inefficient algorithms (e.g., O(n²) when O(n log n) is possible).
- Large memory allocations (e.g., loading entire files into memory).

#### 4. Maintainability

- Methods >30 lines: ask for refactoring.
- Classes with too many responsibilities: suggest extraction.
- Unclear naming: ask for clarification or renaming.
- Missing tests: require tests for new business logic.
- Comments that restate code: ask them to be removed (code should be clear).

#### 5. Consistency

- Check against existing patterns in the codebase.
- Inconsistent error handling: flag for standardization.
- Inconsistent naming: suggest alignment with conventions.

### Review Process

1. **Read the PR description**: Understand the intent and scope.
2. **Check the test coverage**: Is it adequate? Are edge cases tested?
3. **Run the code locally** (if significant): Build, test, and manually verify behavior.
4. **Ask questions**: "Why?" questions help the developer think through their approach.
5. **Approve only when confident**: Don't approve if you have concerns. Leave comments and request changes.

### Comment Guidelines

- **Be specific**: "This method is too long" → "This method is 45 lines; consider extracting the validation logic into a separate method."
- **Explain why**: "Use `trim()` here to handle whitespace in user input" is clearer than "Use `trim()`."
- **Offer alternatives**: Suggest a better approach, not just the problem.
- **Praise good practices**: Acknowledge when they do something well (e.g., "Nice use of the existing `AbstractAsyncDataView`").

---

## For Architects & Tech Leads

Your job is to **set the direction**, **prevent technical debt**, and **empower the team**.

### Design Reviews

When reviewing designs, RFCs, or major PRs:

1. **DRY violations**: Spot duplicated patterns across the system. Push for centralized solutions early.
2. **Security by design**: Verify security is baked in, not an afterthought.
   - Access control: Who can access what? Is it enforced consistently?
   - Data sensitivity: How is sensitive data handled?
   - Audit trail: Are security-relevant actions logged?
3. **Performance assumptions**: Challenge performance claims. Verify with load testing if critical.
   - Pagination: Is it implemented? Limits in place?
   - Caching: What data should be cached? For how long?
   - Database: Are indexes planned? Query patterns reviewed?
4. **Testability**: Is the design testable? Can we test business logic in isolation?
5. **Maintainability**: Will future developers understand this? Is there sufficient documentation?
6. **Scalability**: How will this design scale? Are there bottlenecks?

### Technical Debt Management

- **Track it**: Identify and document areas of technical debt (usually done via issues with `technical-debt` label).
- **Prioritize it**: Balance feature development with debt repayment. Debt that blocks new features is urgent.
- **Prevent new debt**: Enforce standards through reviews and continuous improvement.
- **Refactor regularly**: Schedule time for refactoring (e.g., 20% of sprint capacity).

### Team Enablement

- **Document decisions**: Capture architectural decisions in ADRs or in files like this one.
- **Share knowledge**: Code reviews are teaching moments. Spread best practices through examples.
- **Foster ownership**: Let developers make decisions within established patterns. Micromanaging reduces productivity.
- **Continuous improvement**: Regularly review what's working and what isn't. Adjust standards as needed.

---

## Code Smell Checklist

**Developers**: Before submitting, ask "Does this code smell?"

| Smell                    | What to look for                            | How to fix                                |
| ------------------------ | ------------------------------------------- | ----------------------------------------- |
| **Large class**          | >300 lines                                  | Split into smaller, focused classes       |
| **Long method**          | >30 lines                                   | Extract helper methods                    |
| **Duplicate code**       | Same logic in 2+ places                     | Extract to shared method/class            |
| **Long parameter list**  | >5 parameters                               | Use objects (e.g., DTOs)                  |
| **Magic numbers**        | Unexplained constants (e.g., `42`)          | Extract to named constant or enum         |
| **Unclear naming**       | Variables/methods named `x`, `temp`, `data` | Use descriptive names matching intent     |
| **Deep nesting**         | 3+ levels of if/for                         | Extract to methods or use early returns   |
| **Dead code**            | Unused variables, methods, or branches      | Remove it                                 |
| **Catch-all exceptions** | `catch (Exception e)`                       | Catch specific exceptions                 |
| **Silent failures**      | Exceptions swallowed without logging        | Always log or rethrow with context        |
| **God object**           | One class doing everything                  | Refactor into smaller, focused classes    |
| **Hardcoded values**     | Configuration baked into code               | Use environment variables or config files |
| **No tests**             | New code without test coverage              | Add tests                                 |

---

## Tools & Automation

### Linting & Static Analysis

- **Checkstyle**: Enforces Java style guide (Google Java Style). Run: `./mvnw checkstyle:check` (API) or `mvn checkstyle:check` (GUI)
- **SpotBugs**: Finds common bugs (null pointers, resource leaks, etc.). Run: `./mvnw spotbugs:check` (API) or `mvn spotbugs:check` (GUI)
- **OWASP Dependency Check**: Scans dependencies for known vulnerabilities. Run: `NVD_API_KEY=<key> ./mvnw verify -Ddependency-check.skip=false` (API)

### Copilot CLI Hooks

Hooks in `.github/hooks/` — active automatically when using the Copilot CLI agent, no installation needed.

- **lint-guard** (`PreToolUse`): blocks Maven commands that try to skip checkstyle or SpotBugs.
- **test-guard** (`Stop`): runs checkstyle + SpotBugs + tests per module before the session ends if Java files were modified.
- **format-on-save** (`PostToolUse`): auto-formats Java files after edits.

### IDE Integration

- Configure your IDE to show checkstyle violations in-editor (SonarLint, Checkstyle plugins).
- Enable code inspections and warnings.
- Format code on save using the project's style settings.

---

## Security Checklist

**Reviewers**: Verify each item before approving security-sensitive changes.

- [ ] No hardcoded secrets (passwords, tokens, API keys)
- [ ] Input validation on all user-provided data
- [ ] No SQL injection (use parameterized queries)
- [ ] Authentication required for non-public endpoints
- [ ] Authorization checks (roles/permissions) in place
- [ ] Sensitive data not logged
- [ ] HTTPS/TLS enforced in production configs
- [ ] CSRF tokens used for state-changing operations (if applicable)
- [ ] Session tokens have reasonable timeouts
- [ ] Error messages don't leak sensitive info
- [ ] Dependencies checked for vulnerabilities (OWASP)

---

## Performance Checklist

**Architects & Code Reviewers**: Verify for database/API-heavy features.

- [ ] Pagination implemented (no unbounded result sets)
- [ ] Filters/search optimized (indexes on filtered columns)
- [ ] No N+1 queries (joins/batch fetches, not loops)
- [ ] Connection pooling configured
- [ ] Caching strategy defined for high-traffic queries
- [ ] Response times acceptable for UX (e.g., <2s for user-facing queries)
- [ ] Resource cleanup (connections, streams closed)
- [ ] Load testing done for critical paths

---

## References

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Quarkus Best Practices](https://quarkus.io/guides/simplified-quarkus-integration)
- [Effective Java by Joshua Bloch](https://www.oreilly.com/library/view/effective-java-3rd/9780134686479/)
- [Clean Code by Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
