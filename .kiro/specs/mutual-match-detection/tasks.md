# Implementation Plan

- [x] 1. Establish the persistence and runtime foundation
- [x] 1.1 Add the durable decision and match schema
  - Create `005-create-matching.sql` with changesets `tinder4dogs:005-create-dog-decision` and `tinder4dogs:006-create-dog-match`.
  - Define named primary-key, foreign-key, and uniqueness constraints, cascades, canonical low/high dog columns, UTC `created_at`, and lookup indexes.
  - Add explicit rollback directives and append the include to the master changelog without changing existing entries.
  - Done when: the migrated database contains both tables and the application can validate the schema at startup.
  - _Boundary: Liquibase schema_
  - _Requirements: 1.3, 2.5, 3.4, 3.7, 6.1_

- [x] 1.2 Provide the application clock
  - Declare a Spring `Clock.systemUTC()` bean because Spring does not provide one automatically.
  - Keep service tests able to pass a fixed clock directly.
  - Done when: Spring can construct matching components with a UTC clock and the project test suite still runs without a database.
  - _Boundary: MatchingClockConfiguration_
  - _Requirements: 3.7_

- [ ] 2. Add persistence ports and adapters
- [ ] 2.1 (P) Persist directed decisions
  - Add the decision entity and `LIKE`/`PASS` value set using plain dog ID columns.
  - Define the narrow decision port for directed lookup, reverse-like lookup, and save; implement it with the JPA adapter and derived queries.
  - Done when: `ddl-auto: validate` accepts the decision mapping and one directed pair has one persisted decision slot.
  - _Boundary: Decision, Decisions, DecisionRepository_
  - _Depends: 1.1_
  - _Requirements: 1.1, 1.2, 1.3, 2.5_

- [ ] 2.2 (P) Persist canonical matches
  - Add the match entity with canonical low/high dog IDs and immutable UTC creation time.
  - Define the narrow match port for pair lookup, conflict-free insertion, and newest-first lookup on either side.
  - Implement insertion with `ON CONFLICT ON CONSTRAINT uk_dog_match_pair DO NOTHING`, followed by a normal pair read.
  - Done when: the mapping validates and the adapter's conflict-free insert targets the named schema constraint.
  - _Boundary: Match, Matches, MatchRepository_
  - _Depends: 1.1_
  - _Requirements: 3.4, 3.6, 3.7, 5.1, 5.3, 6.1_

- [ ] 2.3 (P) Isolate dog existence checks
  - Define the one-method dog existence port and an adapter delegating to the existing dog repository.
  - Keep all new code in the match package and leave the dog package unchanged.
  - Done when: matching services depend on the narrow port rather than the JPA repository surface.
  - _Boundary: KnownDogs_
  - _Requirements: 1.4, 5.5_

- [ ] 3. Implement matching behavior
- [ ] 3.1 Record and revise decisions
  - Validate positive, distinct, existing dog IDs before writing and return distinct unknown-dog and self-decision outcomes.
  - Persist likes and passes independently of age or compatibility scoring; repeated decisions are no-ops and opposite decisions replace the stored value.
  - Keep decision persistence and match creation in one write transaction and preserve rollback on failure.
  - Add hand-written in-memory ports for direct unit tests without a Spring context or database.
  - Done when: tests prove rejected requests write nothing and a directed pair retains only its latest decision.
  - _Boundary: MatchingService, decision test support_
  - _Depends: 2.1, 2.3_
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 2.1, 2.2, 2.5, 6.2, 6.3_

- [ ] 3.2 Detect reciprocal likes and create one match
  - After recording a like, find the reverse like and canonicalize the pair before creating a match.
  - Insert with conflict suppression and always read the canonical pair back, so concurrent losers return the existing match without a poisoned transaction or retry.
  - Use the injected clock for creation time; never create or remove a match for a pass.
  - Add unit tests for both arrival orders, retries, pass-to-like revision, like-to-pass retention, and a pre-existing conflict winner.
  - Done when: every reciprocal scenario returns one stable match with a deterministic creation timestamp and one-sided likes return no match.
  - _Boundary: MatchingService, match test support_
  - _Depends: 2.2, 3.1_
  - _Requirements: 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.3_

- [ ] 3.3 Read a dog's matches
  - Return an unknown-dog outcome for a missing dog and an empty list for an existing dog without matches.
  - Map either-side matches to the other dog and creation time in newest-first order, inside a read-only transaction.
  - Do not inspect compatibility scoring or age when reading established matches.
  - Done when: tests prove ordering, empty results, unknown dogs, both pair sides, and matches involving an unscorable dog.
  - _Boundary: MatchingService, match-list test support_
  - _Depends: 2.2, 2.3_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 4. Integrate the HTTP surface
- [ ] 4.1 Characterize existing score endpoints
  - Add direct controller tests for both existing score paths, statuses, and `dogId`, `name`, and `score` JSON fields, including the unscorable response.
  - Establish the regression baseline before renaming the Kotlin score response type.
  - Done when: the tests pass against the existing controller and fail if its wire contract changes.
  - _Boundary: MatchController_
  - _Requirements: 5.1_

- [ ] 4.2 (P) Submit decisions over HTTP
  - Add validated decision request and response payloads at `POST /api/decisions`.
  - Translate accepted, unknown-dog, self-decision, malformed-input, and persistence-failure outcomes to their specified responses.
  - Include match identity, other dog, and UTC creation time when a like is matched; repeated likes return the same semantic result.
  - Done when: direct controller tests prove status and body mapping for like, pass, new match, existing match, unknown dog, self-decision, malformed input, and failure.
  - _Boundary: DecisionController_
  - _Depends: 3.1, 3.2_
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 2.1, 3.1, 3.2, 4.1, 4.2, 4.3, 6.2_

- [ ] 4.3 Expose the dog's match list and preserve score compatibility
  - Add `GET /api/dogs/{dogId}/matches` with newest-first summaries, empty arrays for dogs without matches, and not-found for unknown dogs.
  - Define the shared match summary payload for the list and decision response.
  - Rename only the Kotlin score response type after the characterization tests; preserve existing score paths, statuses, and JSON names.
  - Done when: list-controller tests pass and the score characterization tests remain green after the rename.
  - _Boundary: DogMatchController, MatchController_
  - _Depends: 3.3, 4.1_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 5. Verify the integrated slice
  - Run `mise run test` and `mise run build` after all implementation and controller tests are present.
  - Start PostgreSQL and the application, verifying Liquibase startup, Hibernate schema validation, decision submission, reciprocal matching, retries, pass revision, match listing, and unknown/self errors through the real HTTP surface.
  - Record the known boundary that unit tests and startup validation do not prove cross-transaction concurrency, database constraint enforcement under races, or restart durability; those require a separately approved database-backed test specification.
  - Done when: the mandated build and test tasks pass and the real service demonstrates the supported decision and match flows without scoring data as a prerequisite.
  - _Boundary: application integration_
  - _Depends: 4.2, 4.3_
  - _Requirements: 1.3, 2.5, 3.4, 3.5, 6.1, 6.2_
