# NeoVM Android

NeoVM is an Android-first virtual machine manager prototype inspired by desktop VM managers.

## Current features
- Create and save VM profiles locally.
- Select a guest OS preset, memory size, and virtual disk capacity.
- Pick an ISO file through Android's document picker.
- Start an ARM64 QEMU process from the bundled runtime and show its serial console.
- Create a persistent QCOW2 virtual disk for each VM profile.
- Stop a running QEMU process from the console screen.
- GitHub Actions workflow builds a debug APK and attaches it as a 14-day artifact.

## Important limitations
- This is an early alpha. The current guest display is a **serial console only**. It does not yet provide a graphical VGA/VNC display, snapshots, suspend/resume, or hardware acceleration.
- Begin with an ARM64 Linux ISO that supports serial-console installation. A typical graphical installer may show no useful output, and Windows GUI installation is not supported/verified.
- The QEMU runtime is fetched from the pinned, archived Pockr project during CI. It has not been independently rebuilt or audited by NeoVM. Third-party notices are bundled in the app assets.
- The app copies the selected ISO into private app storage and creates a sparse QCOW2 disk. Ensure there is enough free storage. The ISO source file is not deleted.
- Emulation may be slow and memory-intensive on a phone. Choose modest RAM settings first; Android may terminate the app if system memory is low.
- APK build success proves compilation only. Actual guest boot on a physical Galaxy A16 has not been tested by this workflow.

## Build
Open the `android` directory in Android Studio, or build on a machine with JDK 17 and Android SDK platform 35 installed. The GitHub Actions workflow downloads the pinned QEMU runtime before building:

```bash
gradle --no-daemon -p android assembleDebug
```

For a local build, first obtain the ARM64 QEMU runtime files from the pinned upstream Pockr commit and place the `.so` files in `android/app/src/main/jniLibs/arm64-v8a/`. The workflow performs this step automatically.

The APK output path is `android/app/build/outputs/apk/debug/app-debug.apk`.
