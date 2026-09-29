# AGENTS.md

Instructions for any coding agent working in this repository.

This file describes **this codebase**: how it is built, how it is laid out, and
what it must never contain. It is reviewed like code and it changes when the
code changes. Nothing personal belongs here — your own review prompts, your
checklists and your working habits travel with you, not with this repository.

## What this is

A small service that matches dogs with each other. Kotlin on Spring Boot,
PostgreSQL for storage, Maven for the build, mise for tool versions and tasks.

## Build and test

Use the project tasks. Do not invent your own command lines.

```bash
mise run build     # compile and package
mise run test      # run the test suite
mise run db        # start the application's PostgreSQL
mise run run       # start the application (needs the database)
```

`mise run test` does not need the database: the test suite is unit-level.

## Layout

```
src/main/kotlin/com/ai4dev/tinder4dogs/
├── Tinder4DogsApplication.kt   entry point
├── dog/                        the dog itself: model, storage, HTTP
└── match/                      matching between two dogs
```

One package per concept. A package owns its model, its persistence and its
HTTP surface. If a change needs both `dog` and `match`, say so in the commit
message rather than quietly coupling them.

## Conventions

- Kotlin official code style. Four spaces, no tabs, no wildcard imports.
- Constructor injection only. No field injection, no `@Autowired` on fields.
- Domain rules live in services, never in controllers and never in entities.
- Public functions that can reject their input do so with `require`, at the
  top, before any work.
- Prefer named constants to inline numbers in scoring and validation logic.
- Every schema change is a Liquibase changeset, written as **plain SQL** under
  `src/main/resources/db/changelog/changes/`, and added to the master index.
  Never `ddl-auto: update`.
- A changeset that has run anywhere is immutable. Fix a mistake by adding the
  next changeset, never by editing the last one: editing changes its checksum
  and the application refuses to start against a database that already ran it.

## Tests

- JUnit 5 and AssertJ.
- Name a test after the behaviour it pins, in backticks, in plain English.
- **An assertion must be able to fail.** A test that only checks a result is
  not null pins nothing, and it is worse than no test because it reads like
  coverage. If you cannot state which change would turn the test red, the
  test is not finished.

## What must never be in this repository

This is a product. It must build, run and ship on a machine that has never
heard of the course, the team, or anyone's personal tooling. Concretely, none
of the following belongs here:

- Personal prompts of any kind — review, commit, test generation.
- Personal command line tools and the wrappers around them.
- Model configuration, routing, credentials, or any client for them.
- Tracing, cost accounting or telemetry aimed at a developer's own tooling
  rather than at this application's behaviour in production.

The test for any file: **could this application still build and ship on a
laptop that has never heard of any of that?** If deleting the file changes
that answer, it belongs here. If it does not, it belongs to whoever wrote it.

<!-- ai4dev:cc-sdd:begin -->
<!-- Added by `ai4dev sdd setup`. Everything above this line is yours. -->

# Agentic SDLC and Spec-Driven Development

Kiro-style Spec-Driven Development on an agentic SDLC

## Project Memory
Project memory keeps persistent guidance (steering, specs notes, component docs) so OpenCode honors your standards each run. Treat it as the long-lived source of truth for patterns, conventions, and decisions.

- Use `.kiro/steering/` for project-wide policies: architecture principles, naming schemes, security constraints, tech stack decisions, api standards, etc.
- Use local `AGENTS.md` files for feature or library context (e.g. `src/lib/payments/AGENTS.md`): describe domain assumptions, API contracts, or testing conventions specific to that folder. OpenCode auto-loads these when working in the matching path.
- Specs notes stay with each spec (under `.kiro/specs/`) to guide specification-level workflows.

## Project Context

### Paths
- Steering: `.kiro/steering/`
- Specs: `.kiro/specs/`

### Steering vs Specification

**Steering** (`.kiro/steering/`) - Guide AI with project-wide rules and context
**Specs** (`.kiro/specs/`) - Formalize development process for individual features

### Active Specifications
- Check `.kiro/specs/` for active specifications
- Use `/kiro-spec-status [feature-name]` to check progress

## Development Guidelines
- Think in English, generate responses in English. All Markdown content written to project files (e.g., requirements.md, design.md, tasks.md, research.md, validation reports) MUST be written in the target language configured for this specification (see spec.json.language).

## Minimal Workflow
- Phase 0 (optional): `/kiro-steering`, `/kiro-steering-custom`
- Discovery: `/kiro-discovery "idea"` — determines action path, writes brief.md + roadmap.md for multi-spec projects
- Phase 1 (Specification):
  - Single spec: `/kiro-spec-quick {feature} [--auto]` or step by step:
    - `/kiro-spec-init "description"`
    - `/kiro-spec-requirements {feature}`
    - `/kiro-validate-gap {feature}` (optional: for existing codebase)
    - `/kiro-spec-design {feature} [-y]`
    - `/kiro-validate-design {feature}` (optional: design review)
    - `/kiro-spec-tasks {feature} [-y]`
  - Multi-spec: `/kiro-spec-batch` — creates all specs from roadmap.md in parallel by dependency wave
- Phase 2 (Implementation): `/kiro-impl {feature} [tasks] [--review required|inline|off]`
  - Without task numbers: autonomous mode (subagent per task + independent review + final validation)
  - With task numbers: manual mode (selected tasks in main context, still reviewer-gated before completion)
  - `--review off` skips task-local review; use it intentionally and keep `/kiro-validate-impl {feature}` as the final quality gate
  - `/kiro-validate-impl {feature}` (standalone re-validation)
- Progress check: `/kiro-spec-status {feature}` (use anytime)

## Skills Structure
Skills are located in `.opencode/skills/kiro-*/SKILL.md`
- Each skill is a directory with a `SKILL.md` file
- Use `/skills` to inspect currently available skills
- Invoke a skill directly with `/kiro-<skill-name>`
- Use skills explicitly requested by the user and skills relevant to the task's domain, including design, accessibility, and UX.
- Select skills from their descriptions or metadata first, then read only the selected skills and the references needed for the task.
- Follow explicit host and project rules and retain required workflow checks. Do not skip relevant skills just because the task is small.
- `kiro-review` — task-local adversarial review protocol used by reviewer subagents
- `kiro-debug` — root-cause-first debug protocol used by debugger subagents
- `kiro-verify-completion` — fresh-evidence gate before success or completion claims

## Development Rules
- 3-phase approval workflow: Requirements → Design → Tasks → Implementation
- Human review required each phase; use `-y` only for intentional fast-track
- Keep steering current and verify alignment with `/kiro-spec-status`
- Follow the user's instructions precisely, and within that scope act autonomously: gather the necessary context and complete the requested work end-to-end in this run, asking questions only when essential information is missing or the instructions are critically ambiguous.

## Steering Configuration
- For spec and implementation work, load the core steering files below from `.kiro/steering/`. Reuse current context rather than rereading unchanged files.
- Load additional steering only when required by project rules or relevant to the task.
- Default files: `product.md`, `tech.md`, `structure.md`
- Custom files are supported (managed via `/kiro-steering-custom`)

<!-- ai4dev:cc-sdd:end -->
