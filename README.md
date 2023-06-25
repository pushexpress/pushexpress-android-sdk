## Push.Express Android SDK

Note: if you need fully unique apps, do [obfuscation guide](/docs/ObfuscationGuide.md), else do all steps below.

### Step 1. Prerequisites

You will need to integrate your Push.Express account with Firebase.

1. Follow [Push.Express integration guide](/docs/PushExpressIntegration.md)

2. Create or use existing app in Android Studio

3. Follow [Firebase Cloud Messaging integration guide](/docs/FcmIntegration.md)

### Step 2. Add the JitPack repository to your build file

Ensure you have the latest Android Studio and Android Gradle Plugin!

In your `settings.gradle`, add the Jitpack repo to repositories list (only if you use Gradle [Centralized Repository Declaration](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration) feature, default for new projects since Android Studio Electric Eel):

```groovy
// settings.gradle (Project Settings) in Android Studio
...
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

<details>
<Summary>Alternatively, if you use old `allprojects` style</Summary>

In your **root-level (project-level)** Gradle file (`<project>/build.gradle`), add the Jitpack repo to repositories list:

```groovy
// build.gradle (Project: My_Application) in Android Studio
...
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
</details>

### Step 3. Add Push.Express SDK dependency

In your **module (app-level)** Gradle file (`<project>/<app-module>/build.gradle`), add the pushexpress-android-sdk dependency:

```groovy
// build.gradle (Module :app) in Android Studio
...
dependencies {
    ...
    implementation 'com.github.pushexpress:pushexpress-android-sdk:1.2.0'
}
```

### Step 4. Add required code

See [How to use Push.Express SDK in your Android Studio project](/docs/UseSDKInYourProject.md)

### Step 5. Build and try to send push

Use [Push.Express Documentation](https://push.express) to learn how to view app installs
on your devices and send notifications.
