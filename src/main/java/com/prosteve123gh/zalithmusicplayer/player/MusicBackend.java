package com.prosteve123gh.zalithmusicplayer.player;

import java.nio.file.Path;

public interface MusicBackend {
    void play(Path file) throws Exception;

    void pause() throws Exception;

    void resume() throws Exception;

    void stop() throws Exception;

    void setVolume(float volume) throws Exception;

    boolean isPlaying();

    void close();
}
