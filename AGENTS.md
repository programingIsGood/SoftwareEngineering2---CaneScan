# AGENTS.md — Session Memory

## Project: CaneScanCRUD

Android Studio project for a CRUD-based cane scanning app.

## Stack
- **Language:** Java
- **Build:** Gradle (Kotlin DSL — `build.gradle.kts`)
- **Min SDK:** (check `build.gradle.kts`)
- **Architecture:** Activity-based (no Jetpack Compose detected)

## Key Source Files
| File | Role |
|---|---|
| `app/src/main/java/com/example/canescan_crud/MainActivity.java` | Main entry point |
| `app/src/main/java/com/example/canescan_crud/LoginActivity.java` | Login screen |
| `app/src/main/java/com/example/canescan_crud/RegisterActivity.java` | Registration screen |

## Source Tree
```
app/src/main/java/com/example/canescan_crud/
├── MainActivity.java
├── LoginActivity.java
└── RegisterActivity.java
```

## Conventions
- Java for all source
- Standard Android Gradle build
- No unit/instrumentation test patterns established yet beyond defaults

## Notes
- AGENTS.md created on project init to persist session state
