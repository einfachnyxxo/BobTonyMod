package dev.onyxx.bobtony.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class BobTony_OneBitcoin {
    private final KeyBinding toggleFlyKey; // Keybinding für den Flugmodus
    private final KeyBinding toggleModeKey; // Keybinding für Modus-Umschaltung (Doppelsprung/Key)
    private boolean wasPressed = false;
    private boolean useDoubleSpace = true; // Standard: Doppelsprung
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastJumpTime = 0; // Zeit des letzten Sprungs
    private boolean wasJumping = false; // Status der Sprungtaste
    private long lastModeSwitchTime = 0; // Letzte Modus-Umschaltung
    private static final long MODE_SWITCH_COOLDOWN = 1000; // Abklingzeit in Millisekunden

    public BobTony_OneBitcoin() {
        // Keybinding für Flugmodus
        toggleFlyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BobTony Fly", // Keybinding-Name
                InputUtil.Type.KEYSYM,  // Art des Inputs
                GLFW.GLFW_KEY_CAPS_LOCK, // Standardtaste
                "BobTony Mod LOL" // Kategorie
        ));

        // Keybinding für Modus-Umschaltung
        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BobTony Toggle Mode", // Keybinding-Name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Standardtaste (M)
                "BobTony Mod LOL" // Kategorie
        ));

        // Client-Tick-Event registrieren
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ClientPlayerEntity player = client.player;

            // Modus-Umschaltung prüfen
            long currentTime = System.currentTimeMillis();
            if (toggleModeKey.isPressed() && currentTime - lastModeSwitchTime > MODE_SWITCH_COOLDOWN) {
                useDoubleSpace = !useDoubleSpace;
                lastModeSwitchTime = currentTime; // Zeitpunkt der letzten Umschaltung aktualisieren
                player.sendMessage(Text.of("Flugmodus: " + (useDoubleSpace ? "Doppelsprung" : "Keybinding")), true);
            }

            // Doppelsprung-Logik
            if (useDoubleSpace) {
                handleDoubleSpaceMode(player);
            } else {
                handleKeyBindingMode(player);
            }
        });
    }

    private void handleDoubleSpaceMode(ClientPlayerEntity player) {
        boolean isJumping = player.input.jumping; // Prüfen, ob die Sprungtaste gedrückt wird

        if (isJumping && !wasJumping) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastJumpTime < 300) { // Doppelsprung erkannt
                toggleFlyMode(player);
            }
            lastJumpTime = currentTime;
        }

        wasJumping = isJumping;
    }

    private void handleKeyBindingMode(ClientPlayerEntity player) {
        boolean isPressed = toggleFlyKey.isPressed();

        if (isPressed && !wasPressed) {
            toggleFlyMode(player);
        }

        wasPressed = isPressed;
    }

    private void toggleFlyMode(ClientPlayerEntity player) {
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
