package dev.einfachyannik.bobtony.mixin;

import dev.einfachyannik.bobtony.utils.gui.Color;
import dev.einfachyannik.bobtony.utils.gui.widgets.RoundedButton;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import java.util.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addButtons(CallbackInfo ci) {

        int l = this.height / 4 + 48;
        RoundedButton singleplayer = new RoundedButton(this.width / 2 - 100, l, 200, 20, Text.literal("Singleplayer"), new Color(255, 255, 255), () -> {
            this.client.setScreen(new SelectWorldScreen(this));
        });
        RoundedButton multiplayer = new RoundedButton(this.width / 2 - 100, l + 24, 200, 20, Text.literal("Multiplayer"), new Color(255, 255, 255), () -> {
            Screen screen = (Screen)(this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            this.client.setScreen(screen);
        });
        
        this.addDrawableChild(singleplayer);
        this.addDrawableChild(multiplayer);

    }

    @Inject(method = "addNormalWidgets", at = @At("HEAD"), cancellable = true)
    private void removeNormalButtons(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        cir.cancel();
    }


}