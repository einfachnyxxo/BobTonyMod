package dev.einfachyannik.bobtony.utils;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class CapeHandler {

    public static SkinTextures bobTexture;
    public static Identifier capeTexture;

    public static SkinTextures getCapes(SkinTextures original, AbstractClientPlayerEntity player){

        capeTexture = Identifier.of("bobtony", "textures/capes/cape.png");

        bobTexture = new SkinTextures(
                original.texture(),
                original.textureUrl(),
                capeTexture,
                capeTexture,
                original.model(),
                original.secure()
        );

        return bobTexture;
    }

}
