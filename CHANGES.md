# Changes

## Fork, November 2026

### Fixes
 * Snake builds again with Java 21 and Maven Central libraries only.
 * Fixed the black screen on start when the bamf window service is missing.
 * Sound devices are found on PipeWire, and sound problems no longer break the device screen.
 * Fixed Snake using two CPU cores after a device was unplugged and plugged back in.
 * Uses your graphics card instead of drawing everything on the CPU.
 * The profile dropdown no longer redraws itself 60 times a second.
 * The tray icon shows again.
 * Side buttons are found on devices whose layout doesn't list their input devices (Naga Trinity).

### Changes
 * Updates now come from this fork on GitHub, installed by the `bin/snake` launcher.
 * Removed the Add-ons screen, the beta updates option and the old installer.
 * Removed the "send anonymous usage statistics" option; nothing is sent.

Older history (up to 2021) is in the git log and at https://github.com/bithatch/snake.
