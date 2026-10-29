# ft_hangouts

`ft_hangouts` is a native Android application for the 42 ft_hangouts project.
It stores contacts on the device. It sends and receives SMS messages.

## Technical stack

| Part | Technology | Use |
| --- | --- | --- |
| Application | Java 17 | Implements the screens and application logic. |
| Android | Android SDK 37 | Builds the application. |
| Compatibility | Minimum API 26; target API 35 | Defines the supported Android versions. |
| Build | Gradle Wrapper 9.4.1; Android Gradle Plugin 9.2.1 | Builds the APK. |
| Data | SQLite and `SQLiteOpenHelper` | Stores contacts and messages on the device. |
| SMS | Android `SmsManager` and `BroadcastReceiver` | Sends and receives SMS messages. |
| User interface | Android platform views and resources | Shows the screens in English or French. |
| Photos and calls | Android document picker and phone intent | Adds contact photos and starts calls. |

The application has no external runtime libraries.

## Requirements

Use a Linux or macOS terminal for the commands in this section.

1. Install JDK 17.
2. Install Android SDK Platform 37.
3. Install Android SDK Build Tools 37 and Android SDK Platform Tools.
4. Install Android Emulator if you will use a virtual device.
5. Use an Android device or a virtual device that supports SMS.

You can install the Android SDK components in Android Studio SDK Manager.
The first build also needs network access to download Gradle and build dependencies.

## Build and run

1. Open a terminal in the project directory.

2. Set the paths to your JDK and Android SDK. Replace the example paths with your paths.

   ```sh
   export JAVA_HOME=/path/to/jdk-17
   export ANDROID_HOME=/path/to/Android/Sdk
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
   ```

3. Start a virtual device in Android Studio Device Manager.

   You can use a physical Android device instead. Enable USB debugging. Connect the device by USB.

4. Make sure that `adb` lists the device.

   ```sh
   adb devices
   ```

5. Build the debug APK. Run this command from the project directory.

   ```sh
   ./gradlew :app:assembleDebug
   ```

6. Install the APK on the device.

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

7. Start the application.

   ```sh
   adb shell am start -n com.hguo.fthangouts/.MainActivity
   ```

You can also select the `ft_hangouts` icon on the device.
The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Use the application

- Add a contact. Enter a name and a phone number. You can also add an email address, an address, and notes.
- Select a contact to see its details. Select **Edit contact** to change it. Select **Delete contact** to remove it.
- Open a conversation to send an SMS message. The conversation shows sent and received messages.
- Open the menu at the top right to change the header color.
- Change the device language to French to use the French interface. Use English to return to the English interface.
- Press Home. Open the application again to see the time when it went to the background.
- Turn the device to use portrait or landscape orientation.
- Select **Choose photo** in a contact form to add a photo.
- Select **Call contact** in contact details to start a call.

The application requests SMS and phone permissions when a function needs them.
An incoming SMS from an unknown number adds a contact. The number becomes the contact name.

The interface uses the visual direction of [Multi-Step Service Request Flow](https://dribbble.com/shots/27188838-Multi-Step-Service-Request-Flow-SaaS-Mobile-UI).
The launcher icon uses the [42 logo geometry](https://commons.wikimedia.org/wiki/File:42_Logo.svg).
