### Setup PushExpress

1. Go to [Firebase Console](https://console.firebase.google.com) and create a new project (or use existing one)

   You can use one project for all your apps.

2. Open Project Settings

   <img src="/docs/images/fcm-project-settings.png" width=50%>

3. Go to Service accounts and save to file `private-key.json` (you can use same for all apps)

   <img src="/docs/images/fcm-private-key-page.png">

4. Integrate your Push.Express App with key

  * Go to your Push.Express account
  * Open existing App settings or create a new App
  * Switch App Type to *PushExpress SDK*

     <img src="/docs/images/px-sdk-switch.png" width=30%>

  * Paste `private-key.json` file to *Firebase Admin SDK private key* textbox

     <img src="/docs/images/px-sdk-fcm-key.png">
