package dev.einfachyannik.bobtony.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    private Identifier font = Identifier.of("bobtony", "iconfont");

    @ModifyArgs(
            method = "renderLabelIfPresent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I"
            )
    )
    private void modifyNameTag(Args args) {
        Text originalText = args.get(0);
        Text newText = Text.empty().append(Text.literal("").setStyle(Style.EMPTY.withFont(font))).append(" ").append(originalText);
        args.set(0, newText);
    }

}
