# Gradle Wrapper Setup

This project requires the Gradle Wrapper JAR file to build.

## Quick Setup (Recommended)

Open the project in Android Studio and it will automatically:
1. Download the Gradle Wrapper JAR
2. Sync the project
3. Download all dependencies

## Manual Setup

If you need to set up manually:

1. Download gradle-wrapper.jar from an existing Android project or Gradle:
   - Copy from any other Android Studio project's `gradle/wrapper/` folder
   - Or download from: https://services.gradle.org/distributions/

2. Place `gradle-wrapper.jar` in `gradle/wrapper/` directory

3. Run: `./gradlew build`

## Alternative: Use Gradle directly

If you have Gradle 8.9 installed:
```bash
gradle wrapper --gradle-version 8.9
./gradlew build
```
