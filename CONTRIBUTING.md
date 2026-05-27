# Contributing to VoxelClient

Thank you for your interest in contributing to VoxelClient.

VoxelClient is a Fabric-based Minecraft utility client for Java Edition 1.21.4 – 1.21.11.

---

# Development Setup

## Requirements

- Java 21
- Gradle
- Git
- Minecraft 1.21.x Development Environment

---

## Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/VoxelClient.git
cd VoxelClient
```

---

## Build

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

---

## Run Client

```bash
./gradlew runClient
```

---

# Pull Request Guidelines

Before submitting a PR:

- Ensure the project builds successfully
- Follow the existing code style
- Keep commits clean and descriptive
- Test changes before submitting
- Avoid unnecessary dependencies

---

# Branch Naming

Recommended branch names:

- feature/xyz
- fix/xyz
- refactor/xyz
- docs/xyz

---

# Commit Style

Examples:

```text
feat(ui): add clickgui animations
fix(render): resolve nametag crash
refactor(config): improve config loading
```

---

# Reporting Bugs

Please include:

- Minecraft version
- Fabric Loader version
- Steps to reproduce
- Crash logs if available

---

# Suggestions

Feature suggestions are welcome through GitHub Issues or Discussions.

---

Thank you for helping improve VoxelClient.
