# Requirements Document

## Introduction

Implements **F-08 Matches and notifications** (Priority P0) from `docs/PRD.md`, scoped to this backend service and limited to the *match* half of that feature.

Private dog owners looking for a playmate (Persona 1) or a mating partner (Persona 2) swipe through nearby dogs and express interest, but interest alone leads nowhere today: this backend keeps no record of a swipe at all. `MatchScoreService` computes a compatibility score and ranks candidates, so "match" in the current system means *a compatibility score*, not *two owners who liked each other*. Ranking is the end of the flow.

This feature records each owner's decision on a candidate dog, detects when interest is reciprocated, and creates a single shared match that both sides can read. Because no owner or account concept exists in this backend yet, a decision is expressed between two dogs: the dog making the decision (the *actor*) and the dog it is made about (the *target*).

**Terminology**
- **Decision** — a like or a pass, recorded from one actor dog towards one target dog. A decision is directional.
- **Match** — the result of two dogs having liked each other. A match is *undirected*: it belongs to the pair, has no owner and no initiator, and both dogs see the same match.
- **Unscorable dog** — a dog whose stored profile data prevents a compatibility score from being computed (today, a negative age).

## Boundary Context

- **In scope**: recording likes and passes durably; revising a previous decision; idempotent re-submission; detecting reciprocity; creating exactly one match per pair; reporting whether a like produced a match; listing a dog's matches; recording when each match was created.
- **Out of scope**: notification delivery of every kind — no push, no device registration, no in-app notification records, no unread state (the match record is the only output of this feature, and the notification half of F-08 is deferred to a later spec); chat after a match (F-09); candidate list composition, radius, and filters (F-05, F-06); changes to the scoring rules (F-04); unmatching, blocking, and reporting (F-10); owner accounts and authentication (F-01).
- **Adjacent expectations**: this feature relies on dog profiles already existing and being addressable by identity. It does **not** own dog profile data, nor the compatibility score. It expects the scoring capability to remain available for ranking candidate lists, but a decision never depends on a score — the two are independent (see Requirement 1.6). Any caller expecting to be *told* about a match must poll the match list or read the response to its own like; nothing in this feature pushes.

## Requirements

### Requirement 1: Recording a decision

**Objective:** As a dog owner, I want my like or pass on another dog to be recorded, so that my expressed interest is not lost and can be reciprocated later.

#### Acceptance Criteria

1. When an owner submits a like from an actor dog towards a target dog, the Matching Service shall record that decision and confirm it was recorded.
2. When an owner submits a pass from an actor dog towards a target dog, the Matching Service shall record that decision and shall not create a match.
3. The Matching Service shall retain every recorded decision across a service restart.
4. If a decision names an actor dog or a target dog that does not exist, then the Matching Service shall reject the decision, report that the dog is unknown, and record nothing.
5. If a decision names the same dog as both actor and target, then the Matching Service shall reject the decision as invalid and record nothing.
6. When a decision involves a dog that is unscorable, the Matching Service shall record the decision normally, because expressed interest is independent of compatibility scoring.

### Requirement 2: Idempotency and revision of a decision

**Objective:** As a dog owner, I want repeated or changed swipes to behave predictably, so that an unreliable connection or a change of mind never produces duplicates or surprises.

#### Acceptance Criteria

1. When the same decision is submitted again for an actor and target pair that already holds that decision, the Matching Service shall leave the recorded state unchanged and report the same outcome as the original submission.
2. When a decision is submitted for an actor and target pair that already holds the opposite decision, the Matching Service shall replace the earlier decision so that only the latest decision for that pair is retained.
3. When a pass is replaced by a like and the target dog has already liked the actor dog, the Matching Service shall create a match for that pair.
4. If a like is replaced by a pass after a match already exists for that pair, then the Matching Service shall retain the existing match, because dissolving a match is out of scope for this feature.
5. The Matching Service shall retain at most one decision for any given actor and target pair.

### Requirement 3: Mutual match detection

**Objective:** As a dog owner, I want a match to be created the moment my interest is reciprocated, so that I know contact is possible.

#### Acceptance Criteria

1. When an actor dog likes a target dog that has already liked the actor dog, the Matching Service shall create a match for that pair.
2. When an actor dog likes a target dog that has not liked the actor dog, the Matching Service shall not create a match.
3. When an actor dog passes on a target dog, the Matching Service shall not create a match regardless of any decision the target dog has recorded.
4. The Matching Service shall create at most one match for any given pair of dogs, irrespective of the order in which the two likes arrive or how many times they are submitted.
5. When two dogs like each other at the same time, the Matching Service shall create exactly one match for that pair.
6. The Matching Service shall represent a match without direction, so that the same match is reported to both dogs in the pair.
7. The Matching Service shall record the moment each match was created, so that matches created over a period can be counted.

### Requirement 4: Immediate feedback on a like

**Objective:** As a dog owner, I want to learn straight away that my like produced a match, so that I can act on it without waiting or refreshing.

#### Acceptance Criteria

1. When a like results in a new match, the Matching Service shall report in its response to that like that a match was created, and shall identify the match.
2. When a like does not result in a match, the Matching Service shall report in its response to that like that no match exists for the pair.
3. When a like is re-submitted for a pair that is already matched, the Matching Service shall report the existing match rather than reporting that no match exists.

### Requirement 5: Reading a dog's matches

**Objective:** As a dog owner, I want to see every dog mine has matched with, so that I can review and follow up on them.

#### Acceptance Criteria

1. When the matches of a given dog are requested, the Matching Service shall return every match in which that dog takes part.
2. The Matching Service shall identify, for each returned match, the other dog in the pair and the moment the match was created.
3. When the matches of a given dog are requested, the Matching Service shall order the returned matches with the most recently created first.
4. When a dog that takes part in no match has its matches requested, the Matching Service shall return an empty result rather than reporting an error.
5. If the matches of a dog that does not exist are requested, then the Matching Service shall report that the dog is unknown.
6. When a dog in a match is unscorable, the Matching Service shall still return that match, because a match already established does not depend on scoring.

### Requirement 6: Reliability of recorded outcomes

**Objective:** As the product team, I want decisions and matches to be durable and consistent, so that the "matches created" metric is trustworthy and no owner loses a match.

#### Acceptance Criteria

1. The Matching Service shall retain every created match across a service restart.
2. If recording a decision cannot be completed, then the Matching Service shall report the failure and shall leave no partial decision and no partial match behind.
3. While the Matching Service is available, it shall answer decision submissions and match queries without requiring any scoring data to be present for the dogs involved.
