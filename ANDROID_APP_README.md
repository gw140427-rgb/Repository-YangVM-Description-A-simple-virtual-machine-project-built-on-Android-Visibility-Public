# NeoVM Android

NeoVM is an Android-first virtual machine manager prototype inspired by desktop VM managers.

## Current features
- Create and save VM profiles locally.
- Select a guest OS preset, memory size, and virtual disk capacity.
- Pick an ISO/image file through Android's document picker and keep its URI reference.
- Remove VM profiles without deleting the original ISO file.
- GitHub Actions workflow to build a debug APK on pushes to `neo-vm-android`.

## Important limitation
This first version is a **VM configuration manager**, not yet a hypervisor. It does not boot ISO images. A real guest OS needs an integrated emulation engine such as an appropriately built QEMU runtime, plus Android-compatible display and input integration. The app deliberately reports this limitation instead of pretending to start a VM.

## Build
Open the `android` directory in Android Studio, or build on a machine with JDK 17 and Android SDK platform 35 installed:

```bash
gradle --no-daemon -p android assembleDebug
```

The APK output path is `android/app/build/outputs/apk/debug/app-debug.apk`.
