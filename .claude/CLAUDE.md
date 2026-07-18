# Claude Code Execution Rules (Mandatory)

## Primary Goal
Implementation first. Act like a senior Android/Kotlin Multiplatform engineer working inside the project.

## Source of Truth
- The project's source code and Gradle configuration are the source of truth.
- Trust the versions declared in `libs.versions.toml`, Gradle files, and build scripts.
- Assume declared dependencies are correctly installed.
- Do not verify APIs that belong to declared dependencies before implementation.

## Project Exploration

Explore the project exactly as a developer would.

Read only the project source that is relevant to the requested task, including when applicable:

### Kotlin Multiplatform
- shared/
- commonMain/
- androidMain/
- iosMain/
- jvmMain/
- desktopMain/
- jsMain/
- wasmJsMain/
- browserMain/
- platform-specific implementations
- navigation/
- presentation/
- data/
- domain/
- ui/
- theme/
- di/
- model/
- repository/

### Android Projects
- app/
- feature modules
- ui/
- presentation/
- navigation/
- data/
- domain/
- components/
- theme/
- ViewModels
- Activities
- Fragments
- Composables
- Manifest
- Resources
- Gradle modules

Understand how the existing codebase is organized before editing.

Only read files that are relevant to the requested feature or refactor.

Do not recursively inspect the entire project unless explicitly requested.

## Dependency Policy

Do NOT inspect

- Gradle caches
- transformed artifacts
- source JARs
- META-INF
- generated sources
- KLIBs
- compiler outputs

Do NOT perform dependency discovery for libraries already declared in the project.

If a required dependency is genuinely missing:

- Search official documentation or trusted sources.
- Determine the correct artifact.
- Verify compatibility with the project's Kotlin, Compose, AGP, and KMP versions.
- Add only the dependencies necessary for the requested feature.
- Briefly explain why each dependency was added.
- Always use the latest version.

## Implementation Workflow

1. Read the relevant project files.
2. Understand the current implementation.
3. Determine exactly what must change.
4. Implement immediately.
5. Make incremental edits.
6. Reuse existing architecture and patterns.
7. Only investigate an API after an actual compilation or compatibility issue.

## Refactoring

When asked to migrate or replace components:

- Search only the relevant source files.
- Identify every usage.
- Produce a migration plan.
- Apply consistent changes across the project.
- Preserve behavior unless instructed otherwise.
- Exclude files explicitly mentioned by the user.

## Code Quality

- Follow existing architecture.
- Keep changes minimal and focused.
- Avoid unrelated refactoring.
- Reuse existing components whenever possible.
- Prefer official APIs.

## Performance

Spend time understanding and editing project code.

Do not spend time proving assumptions that are already stated.

Avoid unnecessary planning, dependency verification, or library inspection.

Focus on implementation.

## Build Policy

Android Studio is the authoritative build environment for this project.

Do **not** run Gradle commands yourself, including but not limited to:

- `gradlew build`
- `gradlew assemble`
- `gradlew compile*`
- `gradlew test`
- `gradlew lint`
- Any module-specific Gradle tasks

Assume that Android Studio will handle:

- Project sync
- Dependency resolution
- Incremental compilation
- Build execution
- Build Analyzer
- Configuration cache
- APK generation

After completing implementation:

- Stop after editing the source code.
- Do not invoke Gradle for verification.
- Wait for Android Studio build output supplied by the user.
- If the user shares compiler or Gradle errors, diagnose and fix only those errors.
- Never repeatedly compile after small edits.

If implementation requires adding a dependency:

- Update `libs.versions.toml`.
- Update the appropriate `build.gradle(.kts)` files.
- Explain why the dependency was added.
- Do not run Gradle to verify it.

Treat Android Studio's build output as the source of truth for compilation status.