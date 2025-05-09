package dev.onyxx.bobtony.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.einfachyannik.bobtony.utils.render.Render3D;
import dev.einfachyannik.bobtony.enums.Modes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static dev.einfachyannik.bobtony.utils.render.Render3D.drawLineToEntity;
import static dev.einfachyannik.bobtony.utils.render.Render3D.getEntityPositionOffsetInterpolated;

public class BobTony_OneBitcoin {
    // Keybindings
    private final KeyBinding toggleModeKey; // Keybinding to switch modes (double jump or key)

    // FINAL VARIABLES
    public static final String PREFIX = "§8[§6BobTony§8] §r";

    //State variables
    private List<String> trackerList = new ArrayList();
    private long lastUpdateTime = System.currentTimeMillis(); // Last time movement was simulated
    private long lastJumpTime = 0; // Timestamp of the last jump
    private boolean wasJumping = false; // Tracks if the jump key was previously pressed
    private long lastModeSwitchTime = 0; // Last mode switch timestamp
    private static final long MODE_SWITCH_COOLDOWN = 1000; // Cooldown time for mode switching in ms
    private float flyspeed = 0.1f; // Default flight speed
    private float strength = 2.0f;
    private float speed = 1.0f;
    public static boolean nofall = false;
    private Modes mode = Modes.NONE; //Mode
    private List<Block> trackerBlockList = new ArrayList();
    private static boolean chestesp = false;
    private static boolean fly = false;

    public BobTony_OneBitcoin() {
        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Switch modes", // Keybinding name
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

            // Handle doubleJump
            handleDoubleJump(player);

            // nofall
            if (nofall && client.player != null && !client.player.getAbilities().flying) {
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        client.player.getX(),
                        client.player.getY(),
                        client.player.getZ(),
                        true,
                        false
                ));

                // Wenn ich immernoch damage in der Luft nehme dann geh ich crashout
                client.player.fallDistance = 0.0f;
            }

        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith(".strength ")){
                handleStrengthCommand(message);
                return false;
            }else if (message.equals(".nofall")) {
                handelNoFallCommand();
                return false; //Cancel die Message für den Server halt (ich geh crashout, fick dich IntelliJ)
            } else if (message.startsWith(".flyspeed ")) {
                handelFlySpeedCommand(message);
                return false;
            } else if (message.equals(".nukeserver")) {
                handleNukeCommand();
                return false;
            } else if (message.startsWith(".speed ")) {
                handleSpeedCommand(message);
                return false;
            } else if (message.startsWith(".tracker ")) {
                handleTracker(message);
                return false;
            } else if (message.equals(".chestesp")) {
                handleChestESP();
                return false;
            } else if (message.equals(".fly")){
                handleFly();
                return false;
            }

            return true; //Maybe ein forceMessage also das mutes nichts mehr bringen
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider vertexConsumer = context.consumers();
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            World world = client.world;

            float tickDelta = context.tickCounter().getTickDelta(false);
            dev.einfachyannik.bobtony.utils.gui.Color color = new dev.einfachyannik.bobtony.utils.gui.Color(255, 0, 0, 1.0F);

            for (Entity entity : client.world.getEntities()) {
                if (trackerList != null) {
                    for (String name : trackerList) {
                        if (Objects.equals(entity.getName(), Text.literal(name))) {
                            Render3D.drawLineToEntity(matrices, client.player, entity, tickDelta, color, 2.0f);
                            if (entity != client.player) {
                                double interpolatedX = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
                                double interpolatedY = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
                                double interpolatedZ = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;

                                Box box = entity.getBoundingBox().offset(
                                        interpolatedX - entity.getX() - cameraPos.x,
                                        interpolatedY - entity.getY() - cameraPos.y,
                                        interpolatedZ - entity.getZ() - cameraPos.z
                                );

                                //Render3D.drawBox(matrices, vertexConsumer.getBuffer(RenderLayer.LINES), box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha, red, green, blue);
                                Render3D.draw3DHitBox(matrices, box, color, 2.0F);

                            }
                        }
                    }
                }
            }

            if (chestesp){
                getTileEntities().forEach(blockEntity ->
                {
                    Box box = new Box(
                            blockEntity.getPos().getX() - cameraPos.x,
                            blockEntity.getPos().getY() - cameraPos.y,
                            blockEntity.getPos().getZ() - cameraPos.z,
                            blockEntity.getPos().getX() + 1 - cameraPos.x,
                            blockEntity.getPos().getY() + 1 - cameraPos.y,
                            blockEntity.getPos().getZ() + 1 - cameraPos.z
                    );
                    if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity) {
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(0, 255, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof BarrelBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(0, 0, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof FurnaceBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(255, 0, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof ShulkerBoxBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(255, 100, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof BeaconBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(0, 255, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof DropperBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(255, 255, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } else if (blockEntity instanceof DispenserBlockEntity) {
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(255, 255, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } else if (blockEntity instanceof EnderChestBlockEntity){
                        dev.einfachyannik.bobtony.utils.gui.Color blockColor = new dev.einfachyannik.bobtony.utils.gui.Color(255, 0, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }

                });


            }

        });

    }

    private void handleFly(){
        MinecraftClient client = MinecraftClient.getInstance();

        if (mode != Modes.FLY){
            mode = Modes.FLY;
            client.player.sendMessage(Text.literal(PREFIX + "Fly: §aOn"), false);
        }else {
            mode = Modes.NONE;
            client.player.sendMessage(Text.literal(PREFIX + "Fly: §cOff"), false);
        }
    }

    private void handleChestESP(){
        MinecraftClient client = MinecraftClient.getInstance();
        chestesp = !chestesp;
        if (chestesp){
            client.player.sendMessage(Text.literal(PREFIX + "ChestESP: §aOn"), false);
        }else {
            client.player.sendMessage(Text.literal(PREFIX + "ChestESP: §cOff"), false);
        }
    }

    private void handleTracker(String message){
        MinecraftClient client = MinecraftClient.getInstance();

        String[] parts = message.split(" ");
        if (parts.length == 3) {
            if (parts[1].equals("add")){
                trackerList.add(parts[2]);
                client.player.sendMessage(Text.literal(PREFIX + "§aAdded " + parts[2] + " to Trackerlist: " + trackerList), false);
            }else if (parts[1].equals("remove")){
                trackerList.remove(parts[2]);
            }
        } else {
            client.player.sendMessage(Text.literal(PREFIX + "§cUsage: .tracker <add|remove> <player_name>"), false);
        }
    }

    public static Stream<BlockEntity> getTileEntities() {
        return getLoadedChunks().flatMap(chunk -> chunk.getBlockEntities().values().stream());
    }

    public static Stream<WorldChunk> getLoadedChunks() {
        int radius = Math.max(2, MinecraftClient.getInstance().options.getClampedViewDistance()) + 3;
        int diameter = radius * 2 + 1;

        ChunkPos center = MinecraftClient.getInstance().player.getChunkPos();
        ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
        ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

        Stream<WorldChunk> stream = Stream.<ChunkPos>iterate(min, pos -> {
            int x = pos.x;
            int z = pos.z;
            x++;

            if (x > max.x) {
                x = min.x;
                z++;
            }

            return new ChunkPos(x, z);

        }).limit((long) diameter * diameter).filter(c -> MinecraftClient.getInstance().world.isChunkLoaded(c.x, c.z)).map(c -> MinecraftClient.getInstance().world.getChunk(c.x, c.z)).filter(Objects::nonNull);

        return stream;
    }

    private void handleSpeedCommand(String message){
        MinecraftClient client = MinecraftClient.getInstance();

        String[] parts = message.split(" ");
        if (parts.length == 2) {
            try {
                speed = Float.parseFloat(parts[1]);
                client.player.setMovementSpeed(0.3f);
                client.player.sendMessage(Text.literal(PREFIX + "Speed: §c" + client.player.getMovementSpeed()), false);
            } catch (NumberFormatException e) {
                client.player.sendMessage(Text.literal(PREFIX + "§cKeine richtige Zahl!"), false);
            }
        } else {
            client.player.sendMessage(Text.literal(PREFIX + "§cUsage: .speed <value>"), false);
        }
    }

    private void handleStrengthCommand(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Alles nach dem command
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            try {
                strength = Float.parseFloat(parts[1]);
                client.player.sendMessage(Text.literal(PREFIX + "Boost Strength: §c" + strength), false);
            } catch (NumberFormatException e) {
                client.player.sendMessage(Text.literal(PREFIX + "§cKeine richtige Zahl!"), false);
            }
        } else {
            client.player.sendMessage(Text.literal(PREFIX + "§cUsage: .strength <value>"), false);
        }
    }

    private void handelNoFallCommand(){
        MinecraftClient client = MinecraftClient.getInstance();
        nofall = !nofall;
        if (nofall){
            client.player.sendMessage(Text.literal(PREFIX + "NoFall: §aOn"), false);
        }else {
            client.player.sendMessage(Text.literal(PREFIX + "NoFall: §cOff"), false);
        }
    }

    private void handelFlySpeedCommand(String message){
        MinecraftClient client = MinecraftClient.getInstance();
        // Alles nach dem command
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            try {
                adjustFlySpeed(client.player, Float.parseFloat(parts[1]));
                client.player.sendMessage(Text.literal(PREFIX + "Flyspeed: §c" + flyspeed), false);
            } catch (NumberFormatException e) {
                client.player.sendMessage(Text.literal(PREFIX + "§cKeine richtige Zahl!"), false);
            }
        } else {
            client.player.sendMessage(Text.literal(PREFIX + "§cUsage: .strength <value>"), false);
        }
    }

    private void handleNukeCommand(){

        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();

        if (server != null) {
            server.getOverworld().getPlayers().forEach(player -> {
                PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(player, client.player.isSneaking());
                client.getNetworkHandler().sendPacket(packet);
            });
        }

    }

    // Handles switching between double jump and keybinding modes
    private void handleModeSwitch(ClientPlayerEntity player) {
        long currentTime = System.currentTimeMillis();
        if (toggleModeKey.isPressed() && currentTime - lastModeSwitchTime > MODE_SWITCH_COOLDOWN) {
            if (mode == Modes.FLY){
                mode = Modes.BOOST;
                player.sendMessage(Text.of(PREFIX +" Fly: §cOff"), false);
                player.sendMessage(Text.of(PREFIX +" Boost: §aOn"), false);
            }else {
                mode = Modes.FLY;
                player.sendMessage(Text.of(PREFIX +" Boost: §cOff"), false);
                player.sendMessage(Text.of(PREFIX +" Fly: §aOn"), false);
            }

            lastModeSwitchTime = currentTime; // Update mode switch timestamp
        }
    }

    // Adjusts the flight speed by the specified amount
    private void adjustFlySpeed(ClientPlayerEntity player, float amount) {
        flyspeed = amount;
        player.getAbilities().setFlySpeed(flyspeed);
        player.sendAbilitiesUpdate();
    }

    // Handles flight activation using the double space mode
    private void handleDoubleJump(ClientPlayerEntity player) {
        boolean isJumping = player.input.playerInput.jump(); // Check if jump key is pressed

        if (isJumping && !wasJumping) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastJumpTime < 300) { // Double jump detected
                if(mode == Modes.FLY) {
                    toggleFlyMode(player);
                }else {
                    boostPlayer(player);
                }
            }
            lastJumpTime = currentTime;
        }

        wasJumping = isJumping;
    }

    //Boost Player
    private void boostPlayer(ClientPlayerEntity apfel) {
        double pitch = apfel.getPitch();
        Vec3d velocity;

        if (pitch > 70) {
            velocity = new Vec3d(0.0, 1.0, 0.0).multiply(strength);
        } else {
            velocity = apfel.getRotationVector().multiply(strength);
        }

        apfel.addVelocity(velocity.x, velocity.y + 0.5, velocity.z);
        apfel.velocityModified = true;
    }

    // Toggles the flight mode for the player
    private void toggleFlyMode(ClientPlayerEntity player) {
        boolean isFlying = !player.getAbilities().flying; // Toggle flight state
        player.getAbilities().flying = isFlying;
        player.getAbilities().setFlySpeed(flyspeed); // Set current flyspeed

        if (isFlying) {
            player.sendMessage(Text.of("Fly: §aEnabled"), true);
            //simulateMovement(player); // Start or manage movement simulation
        } else {
            player.sendMessage(Text.of("Fly: §cDisabled"), true);
        }

        player.sendAbilitiesUpdate(); // Sync abilities with the server
    }

    // Simulates slight movements to prevent being kicked for inactivity while flying
    private void simulateMovement(ClientPlayerEntity player) {
        new Thread(() -> {
            try {
                boolean wasSneaking = player.input.playerInput.sneak(); // Sneak-Status initialisieren
                boolean wasTouchingCeiling = false; // Status für Deckenberührung
                while (player.getAbilities().flying) {
                    boolean isSneaking = player.input.playerInput.sneak();

                    // Bewegung simulieren, wenn nicht sneakt und keine Decke berührt wird
                    if (!isSneaking) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime > 50) {
                            // Simuliere leichte Bewegungen
                            player.networkHandler.sendPacket(
                                    new PlayerMoveC2SPacket.PositionAndOnGround(
                                            player.getX(), player.getY() + 0.05, player.getZ(), true, false
                                    )
                            );
                            player.networkHandler.sendPacket(
                                    new PlayerMoveC2SPacket.PositionAndOnGround(
                                            player.getX(), player.getY(), player.getZ(), true, false
                                    )
                            );
                            lastUpdateTime = currentTime;
                        }
                    }

                    // Aktualisiere den vorherigen Zustand
                    wasSneaking = isSneaking;

                    Thread.sleep(50); // 20 Ticks pro Sekunde simulieren
                }
            } catch (InterruptedException ignored) {
            }
        }).start();
    }
}





