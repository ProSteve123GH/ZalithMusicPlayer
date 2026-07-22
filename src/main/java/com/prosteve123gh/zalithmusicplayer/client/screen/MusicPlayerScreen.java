package com.prosteve123gh.zalithmusicplayer.client.screen;

import com.prosteve123gh.zalithmusicplayer.player.LocalMusicPlayer;
import com.prosteve123gh.zalithmusicplayer.player.MusicTrack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class MusicPlayerScreen extends Screen {
    private static final int LIST_START_Y = 58;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int PAGE_SIZE = 8;

    private final LocalMusicPlayer player = LocalMusicPlayer.getInstance();
    private final int page;

    public MusicPlayerScreen(Text title) {
        this(title, 0);
    }

    public MusicPlayerScreen(Text title, int page) {
        super(title);
        this.page = Math.max(0, page);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int bottomY = height - 28;

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> {
            player.refreshLibrary();
            openPage(0);
        }).dimensions(centerX - 154, 26, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Prev"), button -> player.previous()).dimensions(centerX - 80, 26, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Play/Pause"), button -> player.togglePlayPause()).dimensions(centerX - 26, 26, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Next"), button -> player.next()).dimensions(centerX + 58, 26, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), button -> player.stop()).dimensions(centerX + 112, 26, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(player.isShuffle() ? "Shuffle: ON" : "Shuffle: OFF"),
                button -> {
                    player.setShuffle(!player.isShuffle());
                    button.setMessage(Text.literal(player.isShuffle() ? "Shuffle: ON" : "Shuffle: OFF"));
                }
        ).dimensions(centerX - 154, bottomY, 74, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(player.isRepeat() ? "Repeat: ON" : "Repeat: OFF"),
                button -> {
                    player.setRepeat(!player.isRepeat());
                    button.setMessage(Text.literal(player.isRepeat() ? "Repeat: ON" : "Repeat: OFF"));
                }
        ).dimensions(centerX - 76, bottomY, 72, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Vol -"), button -> player.setVolume(player.getVolume() - 0.1f)).dimensions(centerX + 2, bottomY, 48, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Vol +"), button -> player.setVolume(player.getVolume() + 0.1f)).dimensions(centerX + 54, bottomY, 48, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Page -"), button -> openPage(page - 1)).dimensions(centerX + 106, bottomY, 48, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Page +"), button -> openPage(page + 1)).dimensions(centerX + 158, bottomY, 48, 20).build());

        addTrackButtons();
    }

    private void openPage(int newPage) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new MusicPlayerScreen(title, Math.max(0, newPage)));
        }
    }

    private void addTrackButtons() {
        List<MusicTrack> tracks = player.getTracks();
        int start = page * PAGE_SIZE;
        int end = Math.min(tracks.size(), start + PAGE_SIZE);

        if (start >= tracks.size() && !tracks.isEmpty()) {
            openPage(Math.max(0, (tracks.size() - 1) / PAGE_SIZE));
            return;
        }

        for (int i = start; i < end; i++) {
            MusicTrack track = tracks.get(i);
            int row = i - start;
            int y = LIST_START_Y + (row * LIST_ROW_HEIGHT);

            addDrawableChild(ButtonWidget.builder(Text.literal((i + 1) + ". " + track.title()), button -> {
                player.playIndex(i);
            }).dimensions(10, y - 2, width - 20, 16).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        context.drawTextWithShadow(textRenderer, Text.literal("ZalithMusicPlayer"), centerX - 70, 8, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, Text.literal("Folder: " + player.getMusicRoot()), 10, 10, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Status: " + player.getLastStatus()), 10, 22, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, Text.literal("Current: " + player.getCurrentTrack().map(MusicTrack::title).orElse("None")), 10, 34, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, Text.literal(String.format("Volume: %d%%", Math.round(player.getVolume() * 100.0f))), 10, 46, 0xD0D0D0);

        List<MusicTrack> tracks = player.getTracks();
        int start = page * PAGE_SIZE;
        context.drawTextWithShadow(textRenderer, Text.literal("Tracks (" + tracks.size() + ") page " + (page + 1)), 10, LIST_START_Y - 12, 0xFFFFFF);

        if (tracks.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("Drop MP3 files into the musicplayer folder."), 10, LIST_START_Y, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
