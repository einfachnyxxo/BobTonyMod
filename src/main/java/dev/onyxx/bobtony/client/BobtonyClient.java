package dev.onyxx.bobtony.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class BobtonyClient implements ClientModInitializer {
    private static MinecraftClient instance;

    @Override
    public void onInitializeClient() {
        instance = MinecraftClient.getInstance();
        new BobTony_OneBitcoin();
    }

    public static MinecraftClient getInstance() {
        return instance;
    }




}


// ich liebe java...