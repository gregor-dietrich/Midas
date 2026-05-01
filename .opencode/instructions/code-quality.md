# Code Quality, Maintainability & Security Standards — Midas

Expected of all contributors. Before submitting, run quality gates. Reviewers verify before approving.

## Principles

- Clarity over cleverness. Self-documenting code. Single Responsibility.
- Eliminate duplication immediately. Extract shared logic. No copy-paste.
- Security by default: validate all input, least-privilege, no hardcoded secrets.
- Performance: avoid N+1 queries, use pagination/caching, profile before optimizing.

## Quality Gates

**Never skip checkstyle or SpotBugs.** Checkstyle severity is `error` — violations block build.

```shell
# API
cd midas-api && ./mvnw checkstyle:check spotbugs:check

# GUI
cd midas-gui && ./mvnw checkstyle:check spotbugs:check
```

OWASP dependency-check: `./mvnw verify -Ddependency-check.skip=false` (needs `NVD_API_KEY`).

## Code Smell Checklist

| Smell | Threshold | Fix |
|---|---|---|
| Large class | >300 lines | Split into focused classes |
| Long method | >30 lines | Extract helper methods |
| Duplicate code | Same logic in 2+ places | Extract to shared method/class |
| Long parameter list | >5 params | Use DTOs |
| Magic numbers | Unexplained constants | Named constant or enum |
| Unclear naming | `x`, `temp`, `data` | Descriptive names matching intent |
| Deep nesting | 3+ levels of if/for | Extract methods or early returns |
| Dead code | Unused variables/methods | Remove |
| Catch-all exceptions | `catch (Exception e)` | Catch specific exceptions |
| Silent failures | Swallowed exceptions without logging | Always log or rethrow |
| God object | One class doing everything | Refactor into smaller classes |
| Hardcoded values | Config baked into code | Use environment variables |
| No tests | New logic without coverage | Add tests |

## Security Checklist

- [ ] No hardcoded secrets (passwords, tokens, API keys)
- [ ] Input validation on all user-provided data
- [ ] No SQL injection (parameterized queries only)
- [ ] Authentication required for non-public endpoints
- [ ] Authorization checks (`@RolesAllowed`) in place
- [ ] Sensitive data not logged
- [ ] HTTPS/TLS in production configs
- [ ] Session tokens have reasonable timeouts
- [ ] Error messages don't leak internals (paths, stack traces, SQL)
- [ ] Dependencies checked for CVEs

## Performance Checklist

- [ ] Pagination implemented (no unbounded result sets)
- [ ] Filters/search optimized (indexes on filtered columns)
- [ ] No N+1 queries (joins/batch fetches, not loops)
- [ ] Connection pooling configured
- [ ] Caching strategy defined for high-traffic queries
- [ ] Response times <2s for user-facing queries
- [ ] Resource cleanup (connections, streams closed)

## References

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Quarkus Best Practices](https://quarkus.io/guides/simplified-quarkus-integration)
