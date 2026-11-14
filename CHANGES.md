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
 * The DPI slider follows changes made with the mouse's own DPI buttons.
 * Clicking the shortcut while Snake is in the tray shows its window again.
 * Two-colour Breath no longer swaps its colours every time it loads.
 * The Audio effect's settings screen opens on PipeWire, and audio sources are labelled clearly
   ("Monitor of ..." for what's playing on an output).
 * Audio is no longer offered (and broken) on devices without a key matrix; a saved effect that
   can't run on a device falls back to its software version instead of failing at startup.
 * Fixed a crash when opening Macros on desktops without bamf (Cinnamon etc.): the libwnck
   window list was read wrongly and Snake overwrote libwnck's window objects.
 * The tray menu on Cinnamon now closes when you click somewhere else.

### New
 * Spectrum and Breath effects for devices whose driver only does static colour (Naga Trinity).
   Snake animates them itself, so they run while Snake is running (it stays in the tray).
 * Audio effect for those devices too: the lights follow the sound (pick "Monitor of ..." as the
   source to follow what your PC is playing).
 * Mouse profiles saved on the mouse itself (Razer mice with onboard memory): buttons, DPI stages
   and lighting for each profile, readable and writable without Synapse.
 * New look: one window with a menu (Devices, Buttons, Performance, Lighting, Macros, Settings),
   the mouse pictured with its buttons labelled, and the system window frame (F11 for fullscreen).
 * Macros page: list and edit macros, macro sets, in the new window.

### Changes
 * Updates now come from this fork on GitHub, installed by the `bin/snake` launcher.
 * Removed the Add-ons screen, the beta updates option and the old installer.
 * Removed the "send anonymous usage statistics" option; nothing is sent.
 * Removed all the old screens (device details, options, effect option screens, layout designer,
   classic macro editor, custom-frame window). Their settings are on the new pages.
   Per-app macro lists and macro recording are gone for now.

Older history (up to 2021) is in the git log and at https://github.com/bithatch/snake.
