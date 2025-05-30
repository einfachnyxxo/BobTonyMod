package dev.einfachyannik.bobtony.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.einfachyannik.bobtony.utils.gui.Color;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class Render3D {
	public static void draw3DHitBox(MatrixStack matrices, Box box, Color color, float lineThickness) {
		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();

		RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
		RenderSystem.lineWidth(lineThickness);

		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();

		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.minZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.maxZ, (float) box.minX, (float) box.minY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.maxZ, (float) box.minX, (float) box.minY, (float) box.minZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.minX, (float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.maxZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.maxZ, (float) box.minX, (float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.maxY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.maxY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.maxX, (float) box.maxY, (float) box.maxZ, (float) box.minX, (float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrices, bufferBuilder, (float) box.minX, (float) box.maxY, (float) box.maxZ, (float) box.minX, (float) box.maxY, (float) box.minZ, color);

		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		RenderSystem.enableCull();
		RenderSystem.lineWidth(1f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	public static void draw3DBox(MatrixStack matrixStack, Box box, Color color, float lineThickness) {
		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

		MatrixStack.Entry entry = matrixStack.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();

		Tessellator tessellator = RenderSystem.renderThreadTesselator();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();

		RenderSystem.setShader(ShaderProgramKeys.POSITION);

		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), 0.5F);

		BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.maxZ);

		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.minZ);

		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.minZ);

		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.maxZ);

		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.minY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.maxZ);

		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.minZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.minY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.maxZ);
		bufferBuilder.vertex(matrix4f, (float) box.minX, (float) box.maxY, (float) box.minZ);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

		RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

		RenderSystem.lineWidth(lineThickness);

		bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX,
				(float) box.minY, (float) box.minZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX,
				(float) box.minY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.maxZ, (float) box.minX,
				(float) box.minY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.maxZ, (float) box.minX,
				(float) box.minY, (float) box.minZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.minX,
				(float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX,
				(float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.minY, (float) box.maxZ, (float) box.maxX,
				(float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.maxZ, (float) box.minX,
				(float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.maxY, (float) box.minZ, (float) box.maxX,
				(float) box.maxY, (float) box.minZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.maxY, (float) box.minZ, (float) box.maxX,
				(float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.maxX, (float) box.maxY, (float) box.maxZ, (float) box.minX,
				(float) box.maxY, (float) box.maxZ, color);
		buildLine3d(matrixStack, bufferBuilder, (float) box.minX, (float) box.maxY, (float) box.maxZ, (float) box.minX,
				(float) box.maxY, (float) box.minZ, color);

		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

		RenderSystem.enableCull();
		RenderSystem.lineWidth(1f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();

	}

	public static void drawLineToEntity(MatrixStack matrices, PlayerEntity player, Entity entity, float delta, Color color, float lineWidth) {
		Vec3d playerPos = getEntityPositionInterpolated(player, delta);
		Vec3d entityPos = getEntityPositionInterpolated(entity, delta);


		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

		float x1 = (float) (playerPos.x - cameraPos.x);
		float y1 = (float) (playerPos.y - cameraPos.y);
		float z1 = (float) (playerPos.z - cameraPos.z);

		float x2 = (float) (entityPos.x - cameraPos.x);
		float y2 = (float) (entityPos.y - cameraPos.y);
		float z2 = (float) (entityPos.z - cameraPos.z);

		Render3D.drawLine3D(matrices, x1, y1, z1, x2, y2, z2, color, 5.0f);
	}

	public static void drawLineToBlockEntity(MatrixStack matrices, PlayerEntity player, BlockEntity blockEntity, float delta, Color color, float lineWidth) {
		Vec3d playerPos = getEntityPositionInterpolated(player, delta);
		Vec3d blockPos = new Vec3d(
				blockEntity.getPos().getX() + 0.5,
				blockEntity.getPos().getY() + 0.5,
				blockEntity.getPos().getZ() + 0.5
		);

		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

		float x1 = (float) (playerPos.x - cameraPos.x);
		float y1 = (float) (playerPos.y - cameraPos.y);
		float z1 = (float) (playerPos.z - cameraPos.z);

		float x2 = (float) (blockPos.x - cameraPos.x);
		float y2 = (float) (blockPos.y - cameraPos.y);
		float z2 = (float) (blockPos.z - cameraPos.z);

		Render3D.drawLine3D(matrices, x1, y1, z1, x2, y2, z2, color, lineWidth);
	}

	public static Vec3d rotateVector(Vec3d vec, double yaw, double pitch) {
		double cosYaw = Math.cos(Math.toRadians(-yaw));
		double sinYaw = Math.sin(Math.toRadians(-yaw));

		double rotatedX = cosYaw * vec.x - sinYaw * vec.z;
		double rotatedZ = sinYaw * vec.x + cosYaw * vec.z;

		double cosPitch = Math.cos(Math.toRadians(-pitch));
		double sinPitch = Math.sin(Math.toRadians(-pitch));

		double rotatedY = cosPitch * vec.y - sinPitch * rotatedZ;
		rotatedZ = sinPitch * vec.y + cosPitch * rotatedZ;

		return new Vec3d(rotatedX, rotatedY, rotatedZ);
	}

	public static void drawLine3D(MatrixStack matrixStack, Vec3d pos1, Vec3d pos2, Color color, float lineWidth) {
		drawLine3D(matrixStack, (float) pos1.x, (float) pos1.y, (float) pos1.z, (float) pos2.x, (float) pos2.y,
				(float) pos2.z, color, lineWidth);
	}

	public static void drawLine3D(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2,
			Color color, float lineWidth) {

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();

		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

		RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
		RenderSystem.lineWidth(lineWidth);

		BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
		buildLine3d(matrices, bufferBuilder, x1, y1, z1, x2, y2, z2, color);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
		RenderSystem.enableCull();
		RenderSystem.lineWidth(1f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	public static void drawWorldText(WorldRenderContext context, String text, Vec3d entityPos, Color color, float scale) {
		MinecraftClient client = MinecraftClient.getInstance();
		MatrixStack matrices = context.matrixStack();
		Vec3d cameraPos = context.camera().getPos();
		VertexConsumerProvider.Immediate consumerProvider = (VertexConsumerProvider.Immediate) context.consumers();
		DrawContext drawContext = new DrawContext(client, consumerProvider);

		// Berechnung der relativen Position zur Kamera
		double x = entityPos.x - cameraPos.x;
		double y = entityPos.y - cameraPos.y;
		double z = entityPos.z - cameraPos.z;

		// Wenn das Entity nicht sichtbar ist, nicht rendern
		if (z < 0) return;

		matrices.push();

		// Weltposition setzen
		matrices.translate(x, y, z);

		// Kameraausrichtung für Billboarding
		Quaternionf rotation = new Quaternionf()
				.rotateY((float) Math.toRadians(-context.camera().getYaw()))
				.rotateX((float) Math.toRadians(context.camera().getPitch()));

		matrices.multiply(rotation);

		// Skalierung anpassen
		float distance = (float) client.player.squaredDistanceTo(entityPos);
		float textScale = Math.max(0.02F, scale / (distance * 0.1F));
		matrices.scale(-textScale, -textScale, textScale);

		// Text mittig rendern
		drawContext.drawText(client.textRenderer, text, -client.textRenderer.getWidth(text) / 2, 0, color.getColorAsInt(), false);

		matrices.pop();
	}

	private static float getYaw(Direction direction) {
        return switch (direction)
        {
            case SOUTH -> 90.0f;
            case WEST -> 0.0f;
            case NORTH -> 270.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
	}

	private static void buildLine3d(MatrixStack matrices, BufferBuilder bufferBuilder, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();

		Vec3d normalized = new Vec3d(x2 - x1, y2 - y1, z2 - z1).normalize();

		float r = color.getRed();
		float g = color.getGreen();
		float b = color.getBlue();

		bufferBuilder.vertex(matrix4f, x1, y1, z1).color(r, g, b, 1.0f).normal(entry, (float) normalized.x,
				(float) normalized.y, (float) normalized.z);
		bufferBuilder.vertex(matrix4f, x2, y2, z2).color(r, g, b, 1.0f).normal(entry, (float) normalized.x,
				(float) normalized.y, (float) normalized.z);
	}

	/**
	 * Gets the interpolated position of the entity given a tick delta.
	 *
	 * @param entity Entity to get position of
	 * @param delta  Tick delta.
	 * @return Vec3d representing the interpolated position of the entity.
	 */
	public static Vec3d getEntityPositionInterpolated(Entity entity, float delta) {
		return new Vec3d(MathHelper.lerp(delta, entity.prevX, entity.getX()),
				MathHelper.lerp(delta, entity.prevY, entity.getY()),
				MathHelper.lerp(delta, entity.prevZ, entity.getZ()));
	}

	/**
	 * Gets the difference between the interpolated position and
	 *
	 * @param entity Entity to get position of
	 * @param delta  Tick delta.
	 * @return Vec3d representing the interpolated position of the entity.
	 */
	public static Vec3d getEntityPositionOffsetInterpolated(Entity entity, float delta) {
		Vec3d interpolated = getEntityPositionInterpolated(entity, delta);
		return entity.getPos().subtract(interpolated);
	}
}