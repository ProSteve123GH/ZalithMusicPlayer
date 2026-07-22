package com.prosteve123gh.zalithmusicplayer.player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Objects;

public final class AndroidMediaPlayerBackend implements MusicBackend {
    private final Class<?> mediaPlayerClass;
    private final Class<?> completionListenerClass;
    private final Class<?> errorListenerClass;

    private Object mediaPlayer;
    private Runnable completionCallback;

    public AndroidMediaPlayerBackend() throws ClassNotFoundException {
        this.mediaPlayerClass = Class.forName("android.media.MediaPlayer");
        this.completionListenerClass = Class.forName("android.media.MediaPlayer$OnCompletionListener");
        this.errorListenerClass = Class.forName("android.media.MediaPlayer$OnErrorListener");
    }

    public boolean isAvailable() {
        return mediaPlayerClass != null;
    }

    public void setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
    }

    @Override
    public void play(Path file) throws Exception {
        stop();
        mediaPlayer = mediaPlayerClass.getConstructor().newInstance();
        invoke(mediaPlayer, "setDataSource", new Class<?>[]{String.class}, new Object[]{file.toAbsolutePath().toString()});
        try {
            Method setAudioStreamType = mediaPlayerClass.getMethod("setAudioStreamType", int.class);
            setAudioStreamType.invoke(mediaPlayer, 3); // STREAM_MUSIC
        } catch (NoSuchMethodException ignored) {
            // newer Android versions prefer audio attributes
        }
        invoke(mediaPlayer, "setVolume", new Class<?>[]{float.class, float.class}, new Object[]{1.0f, 1.0f});
        hookListeners();
        invoke(mediaPlayer, "prepare", new Class<?>[0], new Object[0]);
        invoke(mediaPlayer, "start", new Class<?>[0], new Object[0]);
    }

    @Override
    public void pause() throws Exception {
        if (mediaPlayer != null) {
            invoke(mediaPlayer, "pause", new Class<?>[0], new Object[0]);
        }
    }

    @Override
    public void resume() throws Exception {
        if (mediaPlayer != null) {
            invoke(mediaPlayer, "start", new Class<?>[0], new Object[0]);
        }
    }

    @Override
    public void stop() throws Exception {
        if (mediaPlayer != null) {
            try {
                invoke(mediaPlayer, "stop", new Class<?>[0], new Object[0]);
            } catch (Throwable ignored) {
                // ignore stop failures on already-stopped players
            }
            try {
                invoke(mediaPlayer, "reset", new Class<?>[0], new Object[0]);
            } catch (Throwable ignored) {
            }
            try {
                invoke(mediaPlayer, "release", new Class<?>[0], new Object[0]);
            } catch (Throwable ignored) {
            }
            mediaPlayer = null;
        }
    }

    @Override
    public void setVolume(float volume) throws Exception {
        if (mediaPlayer != null) {
            float clamped = Math.max(0.0f, Math.min(1.0f, volume));
            invoke(mediaPlayer, "setVolume", new Class<?>[]{float.class, float.class}, new Object[]{clamped, clamped});
        }
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer == null) {
            return false;
        }
        try {
            Object result = invoke(mediaPlayer, "isPlaying", new Class<?>[0], new Object[0]);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            stop();
        } catch (Exception ignored) {
        }
    }

    private void hookListeners() throws Exception {
        if (mediaPlayer == null || completionCallback == null) {
            return;
        }

        Object completionListener = Proxy.newProxyInstance(
                completionListenerClass.getClassLoader(),
                new Class<?>[]{completionListenerClass},
                new CompletionInvocationHandler(completionCallback)
        );
        invoke(mediaPlayer, "setOnCompletionListener", new Class<?>[]{completionListenerClass}, new Object[]{completionListener});

        try {
            Object errorListener = Proxy.newProxyInstance(
                    errorListenerClass.getClassLoader(),
                    new Class<?>[]{errorListenerClass},
                    (proxy, method, args) -> {
                        // swallow errors and report "handled" so MediaPlayer does not crash the app
                        return method.getReturnType().equals(boolean.class);
                    }
            );
            invoke(mediaPlayer, "setOnErrorListener", new Class<?>[]{errorListenerClass}, new Object[]{errorListener});
        } catch (Throwable ignored) {
        }
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private static final class CompletionInvocationHandler implements InvocationHandler {
        private final Runnable completionCallback;

        private CompletionInvocationHandler(Runnable completionCallback) {
            this.completionCallback = Objects.requireNonNull(completionCallback, "completionCallback");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("onCompletion".equals(method.getName())) {
                completionCallback.run();
            }
            return null;
        }
    }
}
