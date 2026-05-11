# Code Quality, Maintainability & Security Standards — Midas

Expected of all contributors. Before submitting, run quality gates. Reviewers verify before approving.

## Principles

- Clarity over cleverness. Self-documenting code. Single Responsibility.
- Eliminate duplication immediately. Extract shared logic. No copy-paste.
- Security by default: validate all input, least-privilege, no hardcoded secrets.
- Performance: avoid N+1 queries, use pagination/caching, profile before optimizing.

## Quality Gates

### Java (API & GUI)
**Strict enforcement of static analysis.** All tools are configured with "fail on violation".

```shell
# API & GUI (run in module directory)
./mvnw compile                           # Triggers Error Prone & NullAway
./mvnw checkstyle:check                  # Google Java Style
./mvnw spotbugs:check                    # Bug patterns
./mvnw pmd:check pmd:cpd-check           # Code smells & duplication
```

**Shortcut (Root Makefile):**
```shell
make lint                                # Runs all Java & Python linters
```

**Quality Standards:**
- **Error Prone & NullAway**: Build-time. Zero warnings allowed. Null safety enforced.
- **Checkstyle**: severity=error. Blocks build on style violations.
- **SpotBugs**: effort=max, threshold=low.
- **PMD/CPD**: Custom ruleset. Duplicate code detection (CPD).
- **Tests**: Mandatory. Build fails if tests fail or coverage drops below thresholds.

### Python (Integration Test)
**Use Ruff for linting and formatting.**

```shell
cd midas-test
ruff check .
```

OWASP dependency-check: `./mvnw verify -Ddependency-check.skip=false` (needs `NVD_API_KEY`).

## Code Smell Checklist

| Smell | Threshold | Fix |
|---|---|---|
| Large class / File | >300 lines | Split into focused classes/modules |
| Long method / Function | >30 lines | Extract helper methods/functions |
| Duplicate code | Same logic in 2+ places | Extract to shared method/class |
| Long parameter list | >5 params | Use DTOs or objects |
| Magic numbers | Unexplained constants | Named constant or enum |
| Unclear naming | `x`, `temp`, `data` | Descriptive names matching intent |
| Deep nesting | 3+ levels of if/for | Extract methods or early returns |
| Dead code | Unused variables/methods | Remove |
| Catch-all exceptions | `catch (Exception e)` / `except Exception:` | Catch specific exceptions |
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
