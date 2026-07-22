package com.prosteve123gh.zalithmusicplayer.player;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class LocalMusicPlayer {
    private static final LocalMusicPlayer INSTANCE = new LocalMusicPlayer();

    private final Path musicRoot;
    private final List<MusicTrack> tracks = new ArrayList<>();
    private final Random random = new Random();
    private final MusicBackend backend;

    private boolean shuffle;
    private boolean repeat;
    private int currentIndex = -1;
    private float volume = 1.0f;
    private String lastStatus = "Idle";

    private LocalMusicPlayer() {
        this.musicRoot = FabricLoader.getInstance().getGameDir().resolve("musicplayer");
        try {
            Files.createDirectories(musicRoot);
        } catch (IOException e) {
            lastStatus = "Failed to create folder: " + e.getMessage();
        }

        MusicBackend candidate;
        try {
            AndroidMediaPlayerBackend androidBackend = new AndroidMediaPlayerBackend();
            androidBackend.setCompletionCallback(() -> {
                // auto-advance on the client thread if possible
                try {
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client != null) {
                        client.execute(this::next);
                    } else {
                        next();
                    }
                } catch (Throwable t) {
                    lastStatus = "Completion callback error: " + t.getMessage();
                }
            });
            candidate = androidBackend;
            lastStatus = "Android MediaPlayer backend ready";
        } catch (Throwable t) {
            candidate = new MusicBackend() {
                @Override public void play(Path file) { lastStatus = "Playback backend unavailable on this runtime"; }
                @Override public void pause() { }
                @Override public void resume() { }
                @Override public void stop() { }
                @Override public void setVolume(float volume) { }
                @Override public boolean isPlaying() { return false; }
                @Override public void close() { }
            };
            lastStatus = "Android MediaPlayer unavailable: " + t.getClass().getSimpleName();
        }
        this.backend = candidate;

        refreshLibrary();
    }

    public static LocalMusicPlayer getInstance() {
        return INSTANCE;
    }

    public Path getMusicRoot() {
        return musicRoot;
    }

    public synchronized List<MusicTrack> getTracks() {
        return Collections.unmodifiableList(new ArrayList<>(tracks));
    }

    public synchronized String getLastStatus() {
        return lastStatus;
    }

    public synchronized Optional<MusicTrack> getCurrentTrack() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) {
            return Optional.empty();
        }
        return Optional.of(tracks.get(currentIndex));
    }

    public synchronized boolean isPlaying() {
        return backend.isPlaying();
    }

    public synchronized boolean isShuffle() {
        return shuffle;
    }

    public synchronized boolean isRepeat() {
        return repeat;
    }

    public synchronized float getVolume() {
        return volume;
    }

    public synchronized void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        lastStatus = "Shuffle " + (shuffle ? "enabled" : "disabled");
    }

    public synchronized void setRepeat(boolean repeat) {
        this.repeat = repeat;
        lastStatus = "Repeat " + (repeat ? "enabled" : "disabled");
    }

    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        try {
            backend.setVolume(this.volume);
            lastStatus = "Volume set to " + Math.round(this.volume * 100.0f) + "%";
        } catch (Throwable t) {
            lastStatus = "Volume failed: " + t.getMessage();
        }
    }

    public synchronized void refreshLibrary() {
        tracks.clear();
        try {
            if (Files.exists(musicRoot)) {
                Files.walk(musicRoot)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".mp3"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                        .forEach(path -> tracks.add(new MusicTrack(path, stripExtension(path.getFileName().toString()))));
            }
            lastStatus = tracks.isEmpty()
                    ? "No MP3 files found in " + musicRoot
                    : "Loaded " + tracks.size() + " tracks";
            if (currentIndex >= tracks.size()) {
                currentIndex = tracks.isEmpty() ? -1 : tracks.size() - 1;
            }
        } catch (IOException e) {
            lastStatus = "Scan failed: " + e.getMessage();
        }
    }

    public synchronized void playIndex(int index) {
        if (index < 0 || index >= tracks.size()) {
            lastStatus = "Track index out of range";
            return;
        }
        currentIndex = index;
        playCurrent();
    }

    public synchronized void playCurrent() {
        if (tracks.isEmpty()) {
            lastStatus = "No tracks to play";
            return;
        }
        if (currentIndex < 0 || currentIndex >= tracks.size()) {
            currentIndex = 0;
        }

        MusicTrack track = tracks.get(currentIndex);
        try {
            backend.play(track.path());
            backend.setVolume(volume);
            lastStatus = "Playing: " + track.title();
        } catch (Throwable t) {
            lastStatus = "Play failed: " + t.getMessage();
        }
    }

    public synchronized void togglePlayPause() {
        try {
            if (backend.isPlaying()) {
                backend.pause();
                lastStatus = "Paused";
            } else if (currentIndex >= 0 && currentIndex < tracks.size()) {
                backend.resume();
                lastStatus = "Resumed";
            } else {
                playCurrent();
            }
        } catch (Throwable t) {
            lastStatus = "Toggle failed: " + t.getMessage();
        }
    }

    public synchronized void stop() {
        try {
            backend.stop();
            lastStatus = "Stopped";
        } catch (Throwable t) {
            lastStatus = "Stop failed: " + t.getMessage();
        }
    }

    public synchronized void next() {
        if (tracks.isEmpty()) {
            lastStatus = "No tracks loaded";
            return;
        }

        if (shuffle) {
            if (tracks.size() == 1) {
                currentIndex = 0;
            } else {
                int next;
                do {
                    next = random.nextInt(tracks.size());
                } while (next == currentIndex);
                currentIndex = next;
            }
        } else {
            currentIndex++;
            if (currentIndex >= tracks.size()) {
                if (repeat) {
                    currentIndex = 0;
                } else {
                    currentIndex = tracks.size() - 1;
                    stop();
                    lastStatus = "Reached end of playlist";
                    return;
                }
            }
        }
        playCurrent();
    }

    public synchronized void previous() {
        if (tracks.isEmpty()) {
            lastStatus = "No tracks loaded";
            return;
        }
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = repeat ? tracks.size() - 1 : 0;
        }
        playCurrent();
    }

    public synchronized void playTrack(MusicTrack track) {
        int idx = tracks.indexOf(track);
        if (idx >= 0) {
            playIndex(idx);
        } else {
            lastStatus = "Track not in library";
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
