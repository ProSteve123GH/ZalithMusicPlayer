# ZalithMusicPlayer

A local MP3 player mod for ZalithLauncher2 / Fabric.

## Features

- Opens a GUI in-game
- Scans `gameDir/musicplayer/`
- Plays local `.mp3` files
- Next / previous / pause / stop
- Shuffle / repeat
- Android MediaPlayer backend via reflection

## Folder layout

Put your music here:

```text
.minecraft/
  musicplayer/
    song1.mp3
    song2.mp3
```

The mod scans that folder recursively.

## Notes

This scaffold is set up for Fabric Loader `0.19.3`, Fabric API `0.154.2`, and Minecraft `26.2`.

The playback backend uses Android's `MediaPlayer` class through reflection so the mod can compile without a hard Android SDK dependency.
If the backend is unavailable, the GUI will still open and the library will still scan.

## Controls

Default keybind: `M`

## Build

```bash
./gradlew build
```
