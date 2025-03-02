package dev.einfachyannik.bobtony.mixin;

import dev.onyxx.bobtony.client.BobTony_OneBitcoin;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;

import static dev.onyxx.bobtony.client.BobTony_OneBitcoin.modules;
import static java.awt.Color.white;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Shadow public abstract void clear();

    @Unique
    private final int padding = 10;

    @Inject(method = "render", at = @At("HEAD"))
    public void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Integer longestModule;
        try {
            longestModule = modules.stream().map(s -> getTextRenderer().getWidth(s)).sorted().toList().getLast();
        } catch (NoSuchElementException ignored) {
            return;
        }
        for (int i = 0; i < modules.size(); i++) {
            context.drawText(getTextRenderer(),
                    modules.get(i),
                    context.getScaledWindowWidth() - padding - longestModule,
                    getTextRenderer().fontHeight + padding * i,
                    white.hashCode(),
                    false);
        }
    }
}
