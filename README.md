# YangVM Manager

A lightweight Android-friendly virtual machine manager prototype.

## What it does
- Create and remove virtual machine profiles
- Start and stop simulated machines
- Configure a machine name, operating system label, and memory allocation
- Save profiles in the browser on this device

## Important
This first version is a **manager UI prototype**. Its Start button simulates a machine state; it does not yet virtualize hardware or boot a real operating system. Real VM execution on Android will be evaluated as a separate milestone.

## Run
Open `index.html` in a modern browser. No build step or paid service is required.

## Roadmap
1. Manager interface and local profiles
2. Machine details and boot log simulation
3. Research and test a real Android-compatible virtualization engine
4. Connect real VM lifecycle controls only if the platform supports them

## Platform
Designed for mobile browsers and Android-first development.
