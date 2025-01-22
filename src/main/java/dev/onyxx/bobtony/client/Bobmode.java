package dev.onyxx.bobtony.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class Bobmode {
    private final KeyBinding toggleFlyKey;
    private boolean wasPressed = false;
    private long lastUpdateTime = System.currentTimeMillis();

    public Bobmode() {
        // Keybinding registrieren
        toggleFlyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bobmode.togglefly", // Keybinding-Name
                InputUtil.Type.KEYSYM,  // Art des Inputs
                GLFW.GLFW_KEY_CAPS_LOCK, // Standardtaste
                "category.bobmode" // Kategorie
        ));

        // Client-Tick-Event registrieren
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Prüfen, ob das Keybinding gedrückt wurde
            boolean isPressed = toggleFlyKey.isPressed();

            if (isPressed && !wasPressed) {
                ClientPlayerEntity player = client.player;
                boolean fly = !player.getAbilities().flying; // Flugmodus toggeln
                player.getAbilities().flying = fly;

                if (fly) {
                    player.sendMessage(Text.of("Flugmodus aktiviert"), true);
                    simulateMovement(player); // Bewegung simulieren
                } else {
                    player.sendMessage(Text.of("Flugmodus deaktiviert"), true);
                }

                // Synchronisiere die Fähigkeiten mit dem Server
                player.sendAbilitiesUpdate();
            }

            wasPressed = isPressed;
        });
    }

    private void simulateMovement(ClientPlayerEntity player) {
        // Simuliere leichte Bewegungen, um Kicks zu vermeiden
        new Thread(() -> {
            try {
                while (player.getAbilities().flying) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime > 50) {
                        // Leichte Bewegung simulieren
                        player.networkHandler.sendPacket(
                                new PlayerMoveC2SPacket.PositionAndOnGround(
                                        player.getX(), player.getY() + 0.05, player.getZ(), true
                                )
                        );
                        player.networkHandler.sendPacket(
                                new PlayerMoveC2SPacket.PositionAndOnGround(
                                        player.getX(), player.getY(), player.getZ(), true
                                )
                        );
                        lastUpdateTime = currentTime;
                    }
                    Thread.sleep(50); // 20 Ticks pro Sekunde simulieren
                }
            } catch (InterruptedException ignored) {
            }
        }).start();
    }
}
