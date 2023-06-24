## PushExpress Android SDK

### Step 1. Prerequisites

You will need to integrate your Push.Express account with Firebase.

1. Follow [Push.Express integration guide](docs/PushExpressIntegration.md)

2. Create or use existing app in AndroidStudio

3. Follow [Firebase Cloud Messaging integration guide](docs/FcmIntegration.md)

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

### Step 3. Add PushExpress SDK dependency

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

```kotlin
import com.pushexpress.sdk.main.SdkPushExpress

const val PUSHEXPRESS_APP_ID = "####-######"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SdkPushExpress.initialize(PUSHEXPRESS_APP_ID)
        SdkPushExpress.setExternalId("<some_external_id>") // optional
        SdkPushExpress.activate() // Don't forget to activate SDK workflow!

        Log.d("Myapp", "PushExpress App Instance Token: " +
                SdkPushExpress.getInstanceToken())
        Log.d("Myapp", "PushExpress App External ID: " +
                SdkPushExpress.getExternalId())
    }
}
```


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
