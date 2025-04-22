package dev.einfachyannik.bobtony.utils.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.einfachyannik.bobtony.utils.gui.Color;
import dev.einfachyannik.bobtony.utils.gui.Rectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class Render2D
{
    public static Vec3d center;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static void updateScreenCenter()
    {
        Vector3f pos = new Vector3f(0, 0, 1);

        if (mc.options.getBobView().getValue())
        {
            MatrixStack bobViewMatrices = new MatrixStack();

            bobView(bobViewMatrices);
            pos.mulPosition(bobViewMatrices.peek().getPositionMatrix().invert());
        }

        center = new Vec3d(pos.x, -pos.y, pos.z)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());
    }

    private static void bobView(MatrixStack matrices)
    {
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();

        if (cameraEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity)
        {
            float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

            float var7 = abstractClientPlayerEntity.distanceMoved - abstractClientPlayerEntity.lastDistanceMoved;
            float g = -(abstractClientPlayerEntity.distanceMoved + var7 * tickDelta);
            float h = MathHelper.lerp(tickDelta, abstractClientPlayerEntity.prevStrideDistance, abstractClientPlayerEntity.strideDistance);

            matrices.translate(MathHelper.sin(g * (float) Math.PI) * h * 0.5F, -Math.abs(MathHelper.cos(g * (float) Math.PI) * h), 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(g * (float) Math.PI) * h * 3f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(g * (float) Math.PI - 0.2f) * h) * 5f));
        }
    }

    public static void drawTexturedQuad(Matrix4f matrix4f, Identifier texture, Rectangle size, Color color)
    {
        drawTexturedQuad(matrix4f, texture, size.getX(), size.getY(), size.getWidth(), size.getHeight(), color);
    }

    public static void drawTexturedQuad(Matrix4f matrix4f, Identifier texture, float x1, float y1, float width,
                                        float height, Color color)
    {
        int colorInt = color.getColorAsInt();

        float x2 = x1 + width;
        float y2 = y1 + height;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(matrix4f, x1, y1, 0).color(colorInt).texture(0, 0);
        bufferBuilder.vertex(matrix4f, x1, y2, 0).color(colorInt).texture(0, 1);
        bufferBuilder.vertex(matrix4f, x2, y2, 0).color(colorInt).texture(1, 1);
        bufferBuilder.vertex(matrix4f, x2, y1, 0).color(colorInt).texture(1, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }


    public static void drawBox(Matrix4f matrix4f, Rectangle size, Color color)
    {
        drawBox(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), color);
    }

    public static void drawBox(Matrix4f matrix4f, float x, float y, float width, float height, Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0).color(colorInt);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawRoundedBox(Matrix4f matrix4f, Rectangle size, float radius, Color color)
    {
        drawRoundedBox(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), radius, color);
    }

    public static void drawRoundedBox(Matrix4f matrix4f, float x, float y, float width, float height, float radius,
                                      Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, color);

        // |---
        bufferBuilder.vertex(matrix4f, x + radius, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(colorInt);

        // ---|
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(colorInt);

        // _||
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(colorInt);

        // |||
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(colorInt);

        /// __|
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);

        // |__
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(colorInt);

        // |||
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(colorInt);

        /// ||-
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);

        /// |-/
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);

        /// /_|
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawCircle(Matrix4f matrix4f, float x, float y, float radius, Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        double roundedInterval = (360.0f / 30.0f);

        for (int i = 0; i < 30; i++)
        {
            double angle = Math.toRadians(0 + (i * roundedInterval));
            double angle2 = Math.toRadians(0 + ((i + 1) * roundedInterval));
            float radiusX1 = (float) (Math.cos(angle) * radius);
            float radiusY1 = (float) Math.sin(angle) * radius;
            float radiusX2 = (float) Math.cos(angle2) * radius;
            float radiusY2 = (float) Math.sin(angle2) * radius;

            bufferBuilder.vertex(matrix4f, x, y, 0).color(colorInt);
            bufferBuilder.vertex(matrix4f, x + radiusX1, y + radiusY1, 0).color(colorInt);
            bufferBuilder.vertex(matrix4f, x + radiusX2, y + radiusY2, 0).color(colorInt);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawTranslucentBlurredRoundedBox(Matrix4f matrix4f, float x, float y, float width, float height,
                                                        float radius, Color color)
    {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (int i = 0; i < 5; i++)
        {
            float r = color.getRed();
            float g = color.getGreen();
            float b = color.getBlue();
            float alpha = color.getAlpha() * (1.0f / (i + 1)); // Adjust alpha for each blur layer

            Color newColor = new Color(r, g, b, alpha);
            drawRoundedBox(matrix4f, x - i, y - i, width + 2 * i, height + 2 * i, radius + i, newColor);
        }

        // Draw the main rounded box
        drawRoundedBox(matrix4f, x, y, width, height, radius, color);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawOutlinedBox(Matrix4f matrix4f, Rectangle size, Color outlineColor, Color backgroundColor)
    {
        drawOutlinedBox(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), outlineColor,
                backgroundColor);
    }

    public static void drawOutlinedBox(Matrix4f matrix4f, float x, float y, float width, float height,
                                       Color outlineColor, Color backgroundColor)
    {

        int backgroundColorInt = backgroundColor.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0).color(backgroundColorInt);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        int outlineColorInt = outlineColor.getColorAsInt();

        bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(outlineColorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawBoxOutline(Matrix4f matrix4f, Rectangle size, Color color)
    {
        drawBoxOutline(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), color);
    }


    public static void drawBoxOutline(Matrix4f matrix4f, float x, float y, float width, float height, Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawRoundedBoxOutline(Matrix4f matrix4f, Rectangle size, float radius, Color color)
    {
        drawRoundedBoxOutline(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), radius, color);
    }

    public static void drawOutlinedRoundedBox(Matrix4f matrix4f, float x, float y, float width, float height,
                                              float radius, Color outlineColor, Color backgroundColor)
    {

        int backgroundColorInt = backgroundColor.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, backgroundColor);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, backgroundColor);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f,
                backgroundColor);
        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, backgroundColor);

        // |---
        bufferBuilder.vertex(matrix4f, x + radius, y, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(backgroundColorInt);

        // ---|
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(backgroundColorInt);

        // _||
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(backgroundColorInt);

        // |||
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(backgroundColorInt);

        /// __|
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);

        // |__
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(backgroundColorInt);

        // |||
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(backgroundColorInt);

        /// ||-
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);

        /// |-/
        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);

        /// /_|
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(backgroundColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(backgroundColorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        int outlineColorInt = outlineColor.getColorAsInt();

        bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        // Top Left Arc and Top
        buildArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, outlineColor);
        bufferBuilder.vertex(matrix4f, x + radius, y, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(outlineColorInt);

        // Top Right Arc and Right
        buildArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, outlineColor);
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height - radius, 0).color(outlineColorInt);

        // Bottom Right
        buildArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f, outlineColor);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height, 0).color(outlineColorInt);

        // Bottom Left
        buildArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, outlineColor);
        bufferBuilder.vertex(matrix4f, x, y + height - radius, 0).color(outlineColorInt);
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(outlineColorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawRoundedBoxOutline(Matrix4f matrix4f, float x, float y, float width, float height,
                                             float radius, Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                VertexFormats.POSITION_COLOR);
        // Top Left Arc and Top
        buildArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, color);
        bufferBuilder.vertex(matrix4f, x + radius, y, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y, 0).color(colorInt);

        // Top Right Arc and Right
        buildArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, color);
        bufferBuilder.vertex(matrix4f, x + width, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height - radius, 0).color(colorInt);

        // Bottom Right
        buildArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f, color);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height, 0).color(colorInt);

        // Bottom Left
        buildArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, color);
        bufferBuilder.vertex(matrix4f, x, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x, y + radius, 0).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawLine(Matrix4f matrix4f, float x1, float y1, float x2, float y2, Color color)
    {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x1, y1, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x2, y2, 0).color(colorInt);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawHorizontalGradient(Matrix4f matrix4f, Rectangle size, Color startColor, Color endColor)
    {
        drawHorizontalGradient(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), startColor,
                endColor);
    }


    public static void drawHorizontalGradient(Matrix4f matrix4f, float x, float y, float width, float height,
                                              Color startColor, Color endColor)
    {
        int startColorInt = startColor.getColorAsInt();
        int endColorInt = endColor.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(startColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0.0F).color(endColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0.0F).color(endColorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0.0F).color(startColorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawVerticalGradient(Matrix4f matrix4f, Rectangle size, Color startColor, Color endColor)
    {
        drawVerticalGradient(matrix4f, size.getX(), size.getY(), size.getWidth(), size.getHeight(), startColor,
                endColor);
    }

    public static void drawVerticalGradient(Matrix4f matrix4f, float x, float y, float width, float height,
                                            Color startColor, Color endColor)
    {
        int startColorInt = startColor.getColorAsInt();
        int endColorInt = endColor.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(startColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y, 0.0F).color(startColorInt);
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0.0F).color(endColorInt);
        bufferBuilder.vertex(matrix4f, x, y + height, 0.0F).color(endColorInt);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawItem(DrawContext drawContext, ItemStack stack, float x, float y)
    {
        drawContext.drawItem(stack, (int) x, (int) y);
    }

    public static void drawString(DrawContext drawContext, String text, Vec3d pos, Color color)
    {

    }

    public static void drawStringWithScale(DrawContext drawContext, String text, float x, float y, Color color,
                                           float scale)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrixStack = drawContext.getMatrices();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0f);
        if (scale > 1.0f)
        {
            matrixStack.translate(-x / scale, -y / scale, 0.0f);
        }
        else
        {
            matrixStack.translate((x / scale) - x, (y * scale) - y, 0.0f);
        }
        drawContext.drawText(client.textRenderer, text, (int) x, (int) y, color.getColorAsInt(), false);
        matrixStack.pop();
    }

    public static void buildFilledArc(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float radius,
                                      float startAngle, float sweepAngle, Color color)
    {
        double roundedInterval = (sweepAngle / radius);

        int colorInt = color.getColorAsInt();

        for (int i = 0; i < radius; i++)
        {
            double angle = Math.toRadians(startAngle + (i * roundedInterval));
            double angle2 = Math.toRadians(startAngle + ((i + 1) * roundedInterval));
            float radiusX1 = (float) (Math.cos(angle) * radius);
            float radiusY1 = (float) Math.sin(angle) * radius;
            float radiusX2 = (float) Math.cos(angle2) * radius;
            float radiusY2 = (float) Math.sin(angle2) * radius;

            bufferBuilder.vertex(matrix, x, y, 0).color(colorInt);
            bufferBuilder.vertex(matrix, x + radiusX1, y + radiusY1, 0).color(colorInt);
            bufferBuilder.vertex(matrix, x + radiusX2, y + radiusY2, 0).color(colorInt);
        }
    }

    public static void drawRoundedBox2(Matrix4f matrix4f, float x, float y, float width, float height, float radius, Color color) {
        int colorInt = color.getColorAsInt();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix4f, x + radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + radius, y + height - radius, 0).color(colorInt);
        bufferBuilder.vertex(matrix4f, x + width - radius, y + height - radius, 0).color(colorInt);

        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f, color);
        buildFilledArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, color);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void buildArc(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float radius,
                                 float startAngle, float sweepAngle, Color color)
    {
        float roundedInterval = (sweepAngle / radius);

        int colorInt = color.getColorAsInt();
        for (int i = 0; i < radius; i++)
        {
            double angle = Math.toRadians(startAngle + (i * roundedInterval));
            float radiusX1 = (float) (Math.cos(angle) * radius);
            float radiusY1 = (float) Math.sin(angle) * radius;

            bufferBuilder.vertex(matrix, x + radiusX1, y + radiusY1, 0).color(colorInt);
        }
    }


    public static int getStringWidth(String text)
    {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        return textRenderer.getWidth(text);
    }
}