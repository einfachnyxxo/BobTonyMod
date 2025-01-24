package dev.onyxx.bobtony.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class BobTony_OneBitcoin {
    // Keybindings
    private final KeyBinding toggleFlyKey; // Keybinding to toggle flight mode
    private final KeyBinding toggleModeKey; // Keybinding to switch modes (double jump or key)
    private final KeyBinding flyspeedMinusKey; // Keybinding to decrease flight speed
    private final KeyBinding flyspeedPlusKey; // Keybinding to increase flight speed

    // State variables
    private boolean wasToggleFlyKeyPressed = false; // Tracks if the toggle fly key was pressed
    private boolean useDoubleSpace = true; // Default mode: double jump
    private long lastUpdateTime = System.currentTimeMillis(); // Last time movement was simulated
    private long lastJumpTime = 0; // Timestamp of the last jump
    private boolean wasJumping = false; // Tracks if the jump key was previously pressed
    private long lastModeSwitchTime = 0; // Last mode switch timestamp
    private static final long MODE_SWITCH_COOLDOWN = 1000; // Cooldown time for mode switching in ms
    private float flyspeed = 0.1f; // Default flight speed
    private boolean wasFlySpeedPlusPressed = false; // Tracks if the increase flyspeed key was pressed
    private boolean wasFlySpeedMinusPressed = false; // Tracks if the decrease flyspeed key was pressed

    public BobTony_OneBitcoin() {
        // Initialize keybindings
        toggleFlyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BobTony Fly", // Keybinding name
                InputUtil.Type.KEYSYM,  // Input type
                GLFW.GLFW_KEY_CAPS_LOCK, // Default key
                "BobTony Mod LOL" // Category
        ));

        flyspeedPlusKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "flyspeed+", // Keybinding name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I, // Default key
                "BobTony Mod LOL" // Category
        ));

        flyspeedMinusKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "flyspeed-", // Keybinding name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U, // Default key
                "BobTony Mod LOL" // Category
        ));

        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BobTony Toggle Mode", // Keybinding name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Default key (M)
                "BobTony Mod LOL" // Category
        ));

        // Register client tick event handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ClientPlayerEntity player = client.player;

            // Handle mode switching
            handleModeSwitch(player);

            // Handle flight speed adjustments
            handleFlySpeedAdjustments(player);

            // Handle flight mode based on the current mode
            if (useDoubleSpace) {
                handleDoubleSpaceMode(player);
            } else {
                handleKeyBindingMode(player);
            }
        });
    }

    // Handles switching between double jump and keybinding modes
    private void handleModeSwitch(ClientPlayerEntity player) {
        long currentTime = System.currentTimeMillis();
        if (toggleModeKey.isPressed() && currentTime - lastModeSwitchTime > MODE_SWITCH_COOLDOWN) {
            useDoubleSpace = !useDoubleSpace; // Toggle mode
            lastModeSwitchTime = currentTime; // Update mode switch timestamp
            player.sendMessage(Text.of("Flugmodus: " + (useDoubleSpace ? "Doppelsprung" : "Keybinding")), true);
        }
    }

    // Handles adjustments to the flight speed
    private void handleFlySpeedAdjustments(ClientPlayerEntity player) {
        boolean isFlySpeedPlusPressed = flyspeedPlusKey.isPressed();
        if (isFlySpeedPlusPressed && !wasFlySpeedPlusPressed) {
            adjustFlySpeed(player, 0.1f); // Increase speed
        }
        wasFlySpeedPlusPressed = isFlySpeedPlusPressed;

        boolean isFlySpeedMinusPressed = flyspeedMinusKey.isPressed();
        if (isFlySpeedMinusPressed && !wasFlySpeedMinusPressed) {
            adjustFlySpeed(player, -0.1f); // Decrease speed
        }
        wasFlySpeedMinusPressed = isFlySpeedMinusPressed;
    }

    // Adjusts the flight speed by the specified amount
    private void adjustFlySpeed(ClientPlayerEntity player, float amount) {
        flyspeed += amount;
        flyspeed = Math.max(0.1f, Math.min(1.0f, flyspeed)); // Clamp speed between 0.1 and 1.0
        player.sendMessage(Text.of("flyspeed updated to: " + String.format("%.1f", flyspeed)), true);
        player.getAbilities().setFlySpeed(flyspeed);
        player.sendAbilitiesUpdate();
    }

    // Handles flight activation using the double space mode
    private void handleDoubleSpaceMode(ClientPlayerEntity player) {
        boolean isJumping = player.input.jumping; // Check if jump key is pressed

        if (isJumping && !wasJumping) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastJumpTime < 300) { // Double jump detected
                toggleFlyMode(player);
            }
            lastJumpTime = currentTime;
        }

        wasJumping = isJumping;
    }

    // Handles flight activation using the toggle fly key
    private void handleKeyBindingMode(ClientPlayerEntity player) {
        boolean isToggleFlyKeyPressed = toggleFlyKey.isPressed();

        if (isToggleFlyKeyPressed && !wasToggleFlyKeyPressed) {
            toggleFlyMode(player);
        }

        wasToggleFlyKeyPressed = isToggleFlyKeyPressed;
    }

    // Toggles the flight mode for the player
    private void toggleFlyMode(ClientPlayerEntity player) {
        boolean isFlying = !player.getAbilities().flying; // Toggle flight state
        player.getAbilities().flying = isFlying;
        player.getAbilities().setFlySpeed(flyspeed); // Set current flyspeed

        if (isFlying) {
            player.sendMessage(Text.of("Flugmodus aktiviert"), true);
            simulateMovement(player); // Start or manage movement simulation
        } else {
            player.sendMessage(Text.of("Flugmodus deaktiviert"), true);
        }

        player.sendAbilitiesUpdate(); // Sync abilities with the server
    }




    // Simulates slight movements to prevent being kicked for inactivity while flying
    private void simulateMovement(ClientPlayerEntity player) {
        new Thread(() -> {
            try {
                boolean wasSneaking = player.input.sneaking; // Track sneaking state

                while (player.getAbilities().flying) {
                    boolean isSneaking = player.input.sneaking;

                    // Simulate movement only if the player is not sneaking
                    if (!isSneaking) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime > 50) {
                            // Simulate slight movement to prevent kicks
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
                    }

                    // Update sneaking state for the next iteration
                    wasSneaking = isSneaking;

                    Thread.sleep(50); // Simulate 20 ticks per second
                }
            } catch (InterruptedException ignored) {
            }
        }).start();
    }





}
