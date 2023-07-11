## How to use Push.Express SDK in your Android Studio project

You should already have all prerequisites from previous steps!

Push.Express SDK dependencies should be added to your project and you should have ready Push.Express App, see main guides first.

### Add required code

1. Get your `PUSHEXPRESS_APP_ID` from [Push.Express](https://push.express) account page

   <img src="/docs/images/px-sdk-app-id.png" width=50%>

2. Add code to your Android Studio app
   ```kotlin
   import com.pushexpress.sdk.main.SdkPushExpress

   const val PUSHEXPRESS_APP_ID = "####-######"

   class MainActivity : AppCompatActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)

           SdkPushExpress.initialize(PUSHEXPRESS_APP_ID)
           SdkPushExpress.setExternalId("<some_external_id>") // optional
           SdkPushExpress.activate() // Don't forget to activate SDK workflow!

           Log.d("Myapp", "App Instance Token: " +
                   SdkPushExpress.getInstanceToken())
           Log.d("Myapp", "App External ID: " +
                   SdkPushExpress.getExternalId())
       }
   }
   ```

3. Ask for notification permissions

   ```kotlin
   // ...
   import android.content.pm.PackageManager
   import android.os.Build
   import android.widget.Toast
   import androidx.activity.result.contract.ActivityResultContracts
   import androidx.core.content.ContextCompat

   class MainActivity : AppCompatActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)

           // ...
           askNotificationPermission()
       }

       private val notificationPermissionLauncher = registerForActivityResult(
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
               if (ContextCompat.checkSelfPermission(this,
                       android.Manifest.permission.POST_NOTIFICATIONS) ==
                   PackageManager.PERMISSION_GRANTED
               ) {
                   // FCM SDK (and your app) can post notifications.
               } else {
                   // Directly ask for the permission
                   notificationPermissionLauncher.launch(
                       android.Manifest.permission.POST_NOTIFICATIONS)
               }
           }
       }
   }
   ```
