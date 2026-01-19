# Project overview
This is a Kotlin project.

# Language & tooling
- Language: Kotlin
- Build system: Gradle (Kotlin DSL)
- IDE: IntelliJ IDEA
- JDK: 21

# Coding standards
- Use idiomatic Kotlin
- Prefer immutable data (val over var)
- Use data classes where appropriate
- Avoid nullable types unless necessary
- Use sealed classes for closed hierarchies
- Prefer coroutines over threads

# Formatting
- Follow official Kotlin style guide
- Use meaningful names
- Avoid long functions (>50 lines)

# Error handling
- Prefer Result or sealed classes over exceptions
- Do not swallow exceptions
- Log errors with context

# What Claude should do
- Suggest clean, readable, production-ready code
- Explain non-trivial decisions briefly
- Do not over-engineer
- Do not introduce new libraries unless necessary
