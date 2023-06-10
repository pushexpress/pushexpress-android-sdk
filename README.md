## PushExpress Android SDK

### Step 1. Requirements

- Push.Express Account with registered App

- Firebase Account with registered App

### Step 2. Add the JitPack repository to your build file

If you use Gradle [Centralized Repository Declaration](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration) feature (default for new projects in Android Studio Electric Eel), in your `settings.gradle`, add the Jitpack repo to repositories list:

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

Alternatively, if you use old `allprojects` style, in your **root-level (project-level)** Gradle file (`<project>/build.gradle`), add the Jitpack repo to repositories list:

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

### Step 3. Add the dependency

In your **module (app-level)** Gradle file (`<project>/<app-module>/build.gradle`), add the pushexpress-android-sdk dependency:

```groovy
// build.gradle (Module :app) in Android Studio
...
dependencies {
    ...
    implementation 'com.github.pushexpress:pushexpress-android-sdk:1.1.3'
}
```

### Step 4. Add required code

```kotlin
import com.pushexpress.sdk.main.SdkPushExpress

const val PUSHEXPRESS_APP_ID = "####-######"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SdkPushExpress.setAppId(PUSHEXPRESS_APP_ID)
        SdkPushExpress.setExternalId("<some_external_id>") // optional

        Log.d("Myapp", "PushExpress App Instance Token: " +
                SdkPushExpress.getInstanceToken())
        Log.d("Myapp", "PushExpress App External ID: " +
                SdkPushExpress.getExternalId())
    }
}
```

### Step 5. Setup Firebase

- Follow Firebase Cloud Messaging [Android guide](https://firebase.google.com/docs/android/setup#console)

- Follow PushExpress [SDK App configuration guide](https://push.express) to connect your PushExpress App with Firebase Cloud Messaging

### Step 6. Ask for notification permissions

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ...
        askNotificationPermission()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                this,
                "FCM can't post notifications without POST_NOTIFICATIONS permission",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

### Step 7. Build and try to send push
Use [PushExpress Documentation](https://push.express) to learn how to view app installs
on your devices and send notifications.
