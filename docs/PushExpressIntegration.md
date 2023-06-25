## Setup Push.Express

### Get Firebase Private key

1. Go to [Firebase Console](https://console.firebase.google.com) and create a new project (or use existing one)

   You can use one project for all your apps.

2. Open Project Settings

   <img src="/docs/images/fcm-project-settings.png" width=50%>

3. Go to Service accounts, press `Generate new private key` and save it to file `private-key.json` (**you can use same key for all apps**)

   <img src="/docs/images/fcm-private-key-page.png">

### Integrate your Push.Express App with Firebase

1. Go to your [Push.Express](https://push.express) account
2. Open existing *App* settings or create a new App
3. Switch *App Type* to *PushExpress SDK*

   <img src="/docs/images/px-sdk-switch.png" width=30%>

4. Paste `private-key.json` file to *Firebase Admin SDK private key* textbox

   <img src="/docs/images/px-sdk-fcm-key.png">

