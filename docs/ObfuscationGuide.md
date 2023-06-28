## Obfuscation

You can obfuscate your app with standard `minifyEnabled true`, but if you want more uniq app code, follow the guide below =)

### Step 1. Prerequisites

You will need to integrate your Push.Express account with Firebase.

1. Follow [Push.Express integration guide](/docs/PushExpressIntegration.md)

2. Create or use existing app in Android Studio

3. Follow [Firebase Cloud Messaging integration guide](/docs/FcmIntegration.md)

### Step 2. Download full SDK repo

1. Download [pushexpress-android-sdk.zip](https://github.com/pushexpress/pushexpress-android-sdk/archive/refs/heads/main.zip)
2. Unzip and rename directory to `pushexpress-android-sdk`
3. Place `pushexpress-android-sdk` near your app dir, for example
```
./
  \- MyApplication
  \- pushexpress-android-sdk
```

### Step 3. Obfuscate SDK locally

**You need Linux (Ubuntu) or MacOS, it will not work on Windows out of the box!**

Open Terminal app and do next steps.

1. Ensure you have installed `Perl`, coreutils `find` and `xargs` commands

   ```
   which perl && which find && which xargs && echo "OK"
   ```

2. In local SDK dir (`pushexpress-android-sdk`) run obfs.pl script

   ```
   cd pushexpress-android-sdk
   ./scripts/obfs.pl
   ```

3. Get new SDK package name, use it in next steps instead of 'com.sdk.pushexpress'

   <img src="/docs/images/obfs-pl.png">

You need to do this step for each app, so **you need to have separate local SDK copy for each app!**

But you don't need (and it is really bad) to repeat this step if you just want to update your app.

### Step 4. Add local SDK dependency
1. In your `settings.gradle` add path to local SDK
   ```
   // settings.gradle (Project Settings) in Android Studio

   include ':sdkpushexpress'
   project(':sdkpushexpress').projectDir = new File('../pushexpress-android-sdk/sdkpushexpress')
   ```

2. In your **module (app-level)** Gradle file (`<project>/<app-module>/build.gradle`), add the SDK dependency
   ```
   // build.gradle (Module :app) in Android Studio

   dependencies {
       ...
       implementation project (":sdkpushexpress")
   }
   ```

3. Run `File->Sync Project with Gradle files` in Android Studio

### Step 5. Add required code

See [How to use Push.Express SDK in your Android Studio project](/docs/UseSDKInYourProject.md)

### Step 6. Enable project obfuscation

1. Enable R8.fullMode in your `gradle.properties`

   ```
   # Project-wide Gradle settings
   ...
   android.enableR8.fullMode=true
   ```

2. In your **module (app-level)** Gradle file (`<project>/<app-module>/build.gradle`), enable obfuscation (`minifyEnabled true`)

   ```
   // build.gradle (Module :app) in Android Studio
   android {
       ...
       buildTypes {
           release {
               minifyEnabled true
       ...
   ```

### Step 7. Build and try to send push

1. Select `Release` build variants for your app and SDK (don't forget to sign you app)
2. `Build->Clean Project`
3. `Build->Build bundle(s) / APK(s)->Build APK(s)`
4. Check obfuscation with decompiler, for example, [jadx](https://github.com/skylot/jadx)
    ```
    cd ./MyApplication/app/build/outputs/apk/release/
    jadx app-release.apk

    grep -ril 'pushexpress' app-release/
    find app-release/ -iname '*pushexpress*'

    # you should see no files here
    ```

Now you can try to send push in you local device or emulator, make sure all works as expected.
