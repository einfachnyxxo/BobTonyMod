package dev.einfachyannik.bobtony.utils.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;

import java.awt.Color;

public class RoundedBoxRenderer {
    public static void drawRoundedBox(Matrix4f matrix4f, float x, float y, float width, float height, float radius, Color color) {
        int colorInt = color.getRGB(); // Farbe als Int-Wert

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Tessellator & Buffer
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        // Ecken mit Kreisbögen rendern
        drawFilledArc(bufferBuilder, matrix4f, x + radius, y + radius, radius, 180.0f, 90.0f, colorInt);
        drawFilledArc(bufferBuilder, matrix4f, x + width - radius, y + radius, radius, 270.0f, 90.0f, colorInt);
        drawFilledArc(bufferBuilder, matrix4f, x + width - radius, y + height - radius, radius, 0.0f, 90.0f, colorInt);
        drawFilledArc(bufferBuilder, matrix4f, x + radius, y + height - radius, radius, 90.0f, 90.0f, colorInt);

        // Mittlere Rechteck-Flächen
        drawRect(bufferBuilder, matrix4f, x + radius, y, width - 2 * radius, height, colorInt);
        drawRect(bufferBuilder, matrix4f, x, y + radius, width, height - 2 * radius, colorInt);

        // Zeichnen
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void drawRect(BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix, x, y, 0).color(color);
        buffer.vertex(matrix, x + width, y, 0).color(color);
        buffer.vertex(matrix, x + width, y + height, 0).color(color);
        buffer.vertex(matrix, x, y + height, 0).color(color);
    }

    private static void drawFilledArc(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius, float startAngle, float arcAngle, int color) {
        int segments = 20; // Anzahl der Segmente für die Rundung
        float angleStep = arcAngle / segments;
        for (int i = 0; i < segments; i++) {
            float theta1 = (float) Math.toRadians(startAngle + i * angleStep);
            float theta2 = (float) Math.toRadians(startAngle + (i + 1) * angleStep);

            float x1 = cx + (float) Math.cos(theta1) * radius;
            float y1 = cy + (float) Math.sin(theta1) * radius;
            float x2 = cx + (float) Math.cos(theta2) * radius;
            float y2 = cy + (float) Math.sin(theta2) * radius;

            buffer.vertex(matrix, cx, cy, 0).color(color);
            buffer.vertex(matrix, x1, y1, 0).color(color);
            buffer.vertex(matrix, x2, y2, 0).color(color);
        }
    }
}