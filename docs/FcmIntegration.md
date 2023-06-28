## Setup Firebase

1. Go to [Firebase Console](https://console.firebase.google.com) and create a new project (or use existing one)

   You can use one project for all your apps.

2. Open Project Settings -> General

   <img src="/docs/images/fcm-project-settings.png" width=50%>

3. Create new Android app or just download `google-services.json` from existing app

   <img src="/docs/images/fcm-apps-settings.png">

   If you need to create new app, just:
   - Register it
   - Download `google-services.json`
   - Press next-next-next =) 

   <img src="/docs/images/fcm-new-android-app.png" width=70%>

3. Put `google-services.json` to your app dir (like `<project>/app/google-services.json`)
4. In top of your **root-level (project-level)** Gradle file (`<project>/build.gradle`), make sure you have the following lines:
   ```
   // build.gradle (Project: My_Application) in Android Studio

   buildscript {
       repositories {
           // Make sure that you have the following two repositories
           google()  // Google's Maven repository
           mavenCentral()  // Maven Central repository
       }
       dependencies {
           // Add the dependency for the Google services Gradle plugin
           classpath 'com.google.gms:google-services:4.3.15'
       }
   }
   ```

5. In your **module (app-level)** Gradle file (`<project>/<app-module>/build.gradle`), add the Firebase Cloud Messaging dependency:
   ```
   // build.gradle (Module :app) in Android Studio

   plugins {
       ...
       id 'com.google.gms.google-services'
   }

   dependencies {
       ...
       implementation platform('com.google.firebase:firebase-bom:32.1.1')
   }
   ```

Full official instructions can be found in [Firebase Cloud Messaging Android guide](https://firebase.google.com/docs/android/setup#console).
