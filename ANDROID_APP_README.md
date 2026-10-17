# NeoVM Android

NeoVM is an Android-first virtual machine manager prototype inspired by desktop VM managers.

## Current features
- Create and save VM profiles locally.
- Select a guest OS preset, memory size, and virtual disk capacity.
- Pick an ISO file through Android's document picker.
- Start an ARM64 QEMU process from the bundled runtime and show its serial console.
- Open the guest's graphical framebuffer in the app through a VNC viewer.
- Send touch-based mouse events and text typed with the Android soft keyboard.
- Create a persistent QCOW2 virtual disk for each VM profile.
- Stop a running QEMU process from the console screen.
- GitHub Actions workflow builds a debug APK and attaches it as a 14-day artifact.

## Important limitations
- This is an early alpha. It provides an in-app RFB/VNC viewer with touch-as-mouse and a text-entry dialog. The VNC server is configured to bind to 127.0.0.1 only, and Android INTERNET permission is used for this loopback socket and QEMU user-mode networking.
- Graphical output depends on the bundled QEMU build including VNC and virtio-gpu support. This code path has not yet been exercised on a physical Galaxy A16; a successful APK build does not prove that an ISO boots or that VNC rendering works on the phone.
- Snapshots, suspend/resume, and hardware acceleration are not implemented. Start with an ARM64 Linux ISO; guest compatibility and performance vary.
- The QEMU runtime is fetched from the pinned, archived Pockr project during CI. It has not been independently rebuilt or audited by NeoVM. Third-party notices are bundled in the app assets.
- The app copies the selected ISO into private app storage, verifies its copied size, and only switches the copy after the new file is complete. It creates a sparse QCOW2 disk. Ensure there is enough free storage. The ISO source file is not deleted.
- VM profiles, copied ISO files, firmware cache, and virtual disks are stored in the app's private data. Uninstalling the app removes these local files. GitHub-hosted debug builds may use different signing keys between runs, so do not uninstall an existing installation that contains data you need to preserve unless you have made a separate backup.
- Emulation may be slow and memory-intensive on a phone. Choose modest RAM settings first; Android may terminate the app if system memory is low.
- Startup now creates firmware and new QCOW2 disks through temporary files, checks existing QCOW2 disks before attaching them, cancels startup if the console activity is closed, and uses a one-time CD boot followed by disk-first boot so installer reboots can proceed to the installed system. These checks reduce stale partial-file and orphan-process problems but do not replace a runtime guest-boot test.
- APK build success proves compilation only. Actual guest boot and VNC rendering on a physical Galaxy A16 have not been tested by this workflow.

## Build
Open the `android` directory in Android Studio, or build on a machine with JDK 17 and Android SDK platform 35 installed. The GitHub Actions workflow downloads the pinned QEMU runtime before building:

```bash
gradle --no-daemon -p android assembleDebug
```

For a local build, first obtain the ARM64 QEMU runtime files from the pinned upstream Pockr commit and place the `.so` files in `android/app/src/main/jniLibs/arm64-v8a/`. The workflow performs this step automatically.

The APK output path is `android/app/build/outputs/apk/debug/app-debug.apk`.
