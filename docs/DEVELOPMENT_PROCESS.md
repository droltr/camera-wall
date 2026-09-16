# Development Process

## Sources of truth

- GitHub Issues define individual features, bugs, acceptance criteria, and dependencies.
- Alpha milestones group Issues into deliverable checkpoints.
- The GitHub Project shows live workflow status and priority.
- Pull requests contain implementation and verification evidence.
- `CHANGELOG.md` records user-visible results.
- GitHub Releases publish tested alpha APKs and checksums.
- The README provides a concise public summary linked to the sources above.

Issue and Project status are authoritative. The README is updated at meaningful
release checkpoints rather than duplicating every small workflow transition.

## Workflow

1. Select the next unblocked Issue in milestone order.
2. Create a branch named `feature/<issue>-<short-name>` from current `main`.
3. Implement small buildable commits scoped to that Issue.
4. Run clean debug and release builds.
5. Back up the known-good tablet APK before risky installation changes.
6. Install and verify the acceptance criteria on the ASUS K012.
7. Open a pull request that contains `Closes #<issue>` and test evidence.
8. Require successful CI before merge.
9. Merge only when the existing camera wall remains operational.
10. Update the changelog and public README status at the alpha checkpoint.

## Status progression

```text
Backlog → Ready → In progress → Device testing → Review → Done
                                              ↘ Blocked
```

Blocked work records the reason and does not bypass an earlier dependency.

## Release progression

The project remains alpha:

```text
v0.2.0-alpha.1
v0.2.0-alpha.2
v0.3.0-alpha.1
```

Beta begins only after the core interface and persistence model survive extended
real-device use. Stable `1.0.0` requires completed security hardening and a
documented recovery path.

## Definition of done

An Issue is done only when:

- Its acceptance criteria pass.
- CI builds successfully.
- Relevant ASUS K012 checks pass.
- No credentials or private network details are committed.
- Documentation and changelog impact are addressed.
- A reviewed pull request is merged into `main`.
