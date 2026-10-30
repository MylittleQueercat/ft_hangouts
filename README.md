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

2. On this computer, load the Android SDK environment and select the installed JDK 17. On another computer, replace these paths with your own JDK and SDK paths.

   ```sh
   . /home/hguo/sgoinfre/hguo/mobile-dev/env.sh
   export JAVA_HOME=/home/hguo/sgoinfre/hguo/mobile-dev/gradle-cache/jdks/eclipse_adoptium-17-amd64-linux.2
   ```

3. Open a second terminal on this computer to start the existing `Pixel_10` emulator. Keep that terminal open while the emulator runs. The `ANDROID_AVD_HOME` override points to where this computer stores its virtual devices.

   ```sh
   . /home/hguo/sgoinfre/hguo/mobile-dev/env.sh
   export ANDROID_AVD_HOME=/home/hguo/sgoinfre/hguo/mobile-dev/android-avd
   emulator -avd Pixel_10
   ```

   On another computer, use `emulator -list-avds` to find an available AVD name, then replace `Pixel_10` with that name. You can also start the emulator from Android Studio Device Manager.

   You can use a physical Android device instead. Enable USB debugging. Connect the device by USB.

4. In the first terminal, wait until `adb` lists the emulator as `device` before continuing. If the list is empty, wait for the emulator to finish booting and try again.

   ```sh
   adb devices
   ```

5. Build the debug APK from the project directory. Continue only after the build succeeds.

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

## Stop the emulator

1. List the running devices.

   ```sh
   adb devices
   ```

2. Find the emulator serial in the list. Replace `emulator-5554` with that serial.

   ```sh
   adb -s emulator-5554 emu kill
   ```

You can also select **Stop** for the device in Android Studio Device Manager.

## Clear local app data

To delete the app's local contacts, messages, and saved preferences, run this command while the device is online:

```sh
adb shell pm clear com.hguo.fthangouts
```

This clears the app's SQLite database and preferences. It does not uninstall the app. Check `adb devices` first. The device must show `device`, not `offline`.

## Test different screen sizes

1. Keep the emulator open with `ft_hangouts` installed. Open a new terminal on the computer, not inside the emulator. Load the Android environment and list the devices.

   ```sh
   . /home/hguo/sgoinfre/hguo/mobile-dev/env.sh
   adb devices
   ```

2. When the list shows `emulator-5554    device`, temporarily set the screen size and density.

   ```sh
   adb shell wm size 720x1280
   adb shell wm density 320
   ```

3. Return to the emulator and check the contact list, contact form, contact details, and conversation screen. Rotate the emulator and check each screen in portrait and landscape orientation.

4. After testing, restore the original settings in the same computer terminal.

   ```sh
   adb shell wm size reset
   adb shell wm density reset
   ```

If `adb devices` lists more than one device, add `-s emulator-5554` after `adb` in each command, using the serial of the emulator you want to test. This setting checks a 360 x 640 dp phone screen; use a large phone or tablet AVD to check larger layouts. Use a phone AVD that supports telephony to test SMS messages and calls.

## Test incoming SMS

You do not need to know the emulator's phone number or use a second phone to test incoming SMS. With the emulator running and USB debugging available, send a simulated message from a computer terminal:

```sh
adb emu sms send 5550101 "Hello from an unknown number"
```

The number is the simulated sender number. If the app has no contact for it, receiving the message creates a contact named `5550101` and adds the message to its conversation. If more than one emulator is running, target one with `adb -s emulator-5554 emu sms send 5550101 "Hello from an unknown number"`.

## Use the application

- Add a contact. Enter a name and a phone number. You can also add an email address, an address, notes, pronouns, and a birthday.
- Use the alphabet index beside the contact list to jump to the first name beginning with that letter. Star a favorite in its details to pin it above the alphabetical list, or mark it as an emergency contact.
- Select a contact to see its details. Select **Edit contact** to change it. Select **Delete contact** to remove it.
- Open a conversation to send an SMS message. The conversation shows sent and received messages.
- Open the menu at the top right to choose a theme. Its gradient, line icon, buttons, highlights, and message bubbles update together.
- Change the device language to French to use the French interface. Use English to return to the English interface.
- Press Home. Open the application again to see the time when it went to the background.
- Turn the device to use portrait or landscape orientation.
- Select **Choose photo** in a contact form to add a photo.
- Select **Call contact** in contact details to start a call.
- Select **Block in app** in contact details to stop calls and outgoing messages here and ignore new SMS from that number in ft_hangouts. The contact and existing conversation remain visible. This does not block SMS or calls system-wide.

The application requests SMS and phone permissions when a function needs them.
An incoming SMS from an unknown number adds a contact. The number becomes the contact name.

The interface uses a near-white canvas, dark text, and restrained accents that follow the selected theme. Primary actions use the theme's gradient. Navigation and actions use native vector icons.
The launcher icon uses the [42 logo geometry](https://commons.wikimedia.org/wiki/File:42_Logo.svg).
