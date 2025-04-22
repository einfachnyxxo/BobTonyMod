package dev.einfachyannik.bobtony.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.einfachyannik.bobtony.utils.render.WorldToScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
	void renderer_postWorldRender(WorldRenderer instance, ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, Operation<Void> original) {
		original.call(instance, allocator, tickCounter, renderBlockOutline, camera, gameRenderer, positionMatrix, projectionMatrix);


		WorldToScreen.lastProjMat.set(RenderSystem.getProjectionMatrix());
		WorldToScreen.lastModMat.set(RenderSystem.getModelViewMatrix());
		WorldToScreen.lastWorldSpaceMatrix.set(positionMatrix);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, WorldToScreen.lastViewport);

		// restore state like the original world rendering code did
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		RenderSystem.setShaderFog(Fog.DUMMY);
	}
}