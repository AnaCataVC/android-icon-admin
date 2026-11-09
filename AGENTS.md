# Agent Guidelines: Android Icons Admin

This document defines architecture invariants, development workflows, quality gates, and release packaging rules for **Android Icons Admin**. All AI agents and automated workflows interacting with this repository must strictly adhere to these directives.

---

## 1. Project Overview & Architecture

**Android Icons Admin** is a native Android application built with Kotlin, Jetpack Compose, and cleanroom software engineering principles. It features a decoupled `:core` module for JVM-only icon processing logic and an `:app` module for Android UI rendering.

- **Stack:**
  - Language: Kotlin 2.0+ (JDK 21)
  - UI: Jetpack Compose (Material 3)
  - Multi-Module: `:core` (JVM domain logic) and `:app` (Android application)
  - Build System: Gradle 8.10+ with Gradle Wrapper

---

## 2. Release & CI/CD Pipeline

- **CI Workflow (`.github/workflows/ci.yml`):**
  - Triggers on `push` to `main` and `pull_request`.
  - Runs JVM tests in `:core:test` and builds debug APK (`:app:assembleDebug`).
  - Does NOT build releases or touch keystores.
- **Release Workflow (`.github/workflows/release.yml`):**
  - Triggers strictly on tag push (`git push origin v*`).
  - Decodes `secrets.RELEASE_KEYSTORE_BASE64`, signs with production credentials, builds `:app:assembleRelease`, and publishes the release on GitHub (GitHub natively computes and displays the SHA-256 digest on the web UI).
  - Cleans up keystores immediately upon completion (`if: always()`).
- **Official Deliverable:**
  - `AndroidIconsAdmin-vX.Y.Z.apk`

---

## 3. Development & Verification Commands

```powershell
# Run core JVM cleanroom tests
.\gradlew :core:test --info

# Assemble debug APK
.\gradlew :app:assembleDebug
```
