package com.prosteve123gh.zalithmusicplayer.client;

import com.prosteve123gh.zalithmusicplayer.client.screen.MusicPlayerScreen;
import com.prosteve123gh.zalithmusicplayer.player.LocalMusicPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MusicPlayerClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LocalMusicPlayer.getInstance();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zalithmusicplayer.open_gui",
                GLFW.GLFW_KEY_M,
                "category.zalithmusicplayer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                openScreen(client);
            }
        });
    }

    private void openScreen(MinecraftClient client) {
        if (client == null) {
            return;
        }
        client.execute(() -> client.setScreen(new MusicPlayerScreen(Text.literal("ZalithMusicPlayer"))));
    }
}
