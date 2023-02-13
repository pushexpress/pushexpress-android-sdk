## PushExpress Android SDK

### Step 1. Requirements

- Push.Express Account with registered App
  
- Firebase Account with registered App
  

### Step 2. Add the JitPack repository to your build file

Open your App `build.gradle (Module: app)` file, add the following to your `dependencies` section:

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 3. Add the dependency

```groovy
dependencies {
    implementation 'com.github.pushexpress:pushexpress-android-sdk:x.y.z'
}
```

Where x.y.z is latest tag from [Tags](https://github.com/pushexpress/pushexpress-android-sdk/tags)

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

- Get `google-services.json` from your Firebase Console and save to app module dir (like app/google-services.json)
  
- Follow guides for App configuration with SDK: https://push.express
  

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
