# ClavinManager

Android app (Kotlin + Jetpack Compose) that moves `.lua` scripts into ClavinTool for you.

## Build the APK with GitHub

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab. The *Build ClavinManager APK* workflow runs on every push (or press **Run workflow**).
3. When it turns green, open the run and download the **ClavinManager-apk** artifact. Unzip it and install `ClavinManager.apk`.

The workflow builds a debug-signed APK, which installs directly on any phone (allow "install unknown apps").

## What it does

**Lua Script Loader**
- First launch only: asks the three setup questions (ClavinTool installed? cloned? which clone app?).
- Answers are remembered, so they are never asked again.
- Pick a `.lua` file (file picker opens on Downloads). It is copied to the `ClavinTool` folder:
  - Clavinbox: `Android/data/com.sirclavin.clavinbox/files/blackbox/storage/emulated/0/ClavinTool/`
  - Multi App Ultra: `Android/data/com.waxmoon.ma.gp/rootfs/storage/emulated/0/ClavinTool/`
  - Other: the app searches the folder you picked for `emulated/0`, then uses `ClavinTool` inside it.
- `ClavinTool` is only created if it does not exist yet. Existing scripts with the same name are replaced.

**Changing your mind**
- Auto-detect (on by default): if the clone app you chose gets uninstalled and the other one is installed, ClavinManager switches for you. If a new clone app appears, it asks whether to switch.
- After the first import a "?" hint explains that answers can be changed in **Settings**.
- Settings lets you edit every answer, re-grant folder access, turn auto-detect off, or reset everything.

## Resource Pack Importer

- Asks once: Real or Cloned Blockmango, and whether Sky Pixel has been loaded (if not, it shows the 4 steps). Answers are remembered.
- Pick a texture pack (zip, rar, 7z, tar, gz, bz2, xz). It is unpacked into Blockmango's `map_temp` folder and replaces any existing files with the same name:
  - Real: `Android/data/com.sandboxol.blockymods/files/Download/SandboxOL/BlockManv2/map_temp/`
  - Clavinbox / Multi App Ultra: the same path inside the clone app's `emulated/0`
  - Other: ClavinManager searches the chosen app folder for `emulated/0/Android/data/com.sandboxol.blockymods/...`
- If the clone app is not known yet, it asks. It never guesses.

## History

Every imported script and pack is listed. Scripts have a bin button that deletes them from the ClavinTool folder, so you never need to go into the folder by hand.

## Folder access (and the "privacy" error)

Android guards other apps' `Android/data` folders. ClavinManager tries, in order:

1. Plain file access (works when "All files access" is on, on phones that allow it).
2. The system folder picker (one-time "Use this folder").
3. If the picker says it can't allow access for privacy reasons, the access pop-up offers **All files access**, and for "Other" apps lets you pick or type the package name.

Honest limit: from Android 11 onward, many phones keep `Android/data` closed even with All files access, and Android 13+ blocks it in the picker too. On those phones neither route can work, and a helper such as Shizuku would be needed.

## Colours

Defined in `design/design.md`: Blue `#2563EB`, Black `#0F172A`, White `#FFFFFF`, Aqua `#2DD4BF`. The launcher icon is built from `design/clavinmanager-icon.svg`.
