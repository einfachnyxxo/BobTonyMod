package dev.einfachyannik.bobtony.mixin;

import dev.onyxx.bobtony.client.BobTony_OneBitcoin;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class FakeLagMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof PlayerMoveC2SPacket && BobTony_OneBitcoin.fakeLag) {
            ci.cancel();

            new Thread(() -> {
                try {
                    Thread.sleep(BobTony_OneBitcoin.fakeLagDelay);
                    ((ClientPlayNetworkHandler) (Object) this).getConnection().send(packet);
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }
}