package dev.einfachyannik.bobtony.utils.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.einfachyannik.bobtony.utils.gui.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.*;
import dev.einfachyannik.bobtony.utils.gui.Color;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static dev.einfachyannik.bobtony.utils.gui.render.Render2D.buildFilledArc;

public class RoundedButton extends ClickableWidget {
    private final Runnable onClick;
    private final Color color;
    private final Color hoverColor;
    private boolean isHovered = false;

    public RoundedButton(int x, int y, int width, int height, Text message, Color color, Runnable onClick) {
        super(x, y, width, height, message);
        this.color = color;
        this.hoverColor = color.add(1.0F, 1.0F, 1.0F);
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isMouseOver(mouseX, mouseY);
        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
        drawRoundedBox(matrix4f, getX(), getY(), getWidth(), getHeight(), 2, isHovered ? hoverColor : color);

        // Text in die Mitte des Buttons rendern
        int textX = getX() + getWidth() / 2;
        int textY = getY() + getHeight() / 2 - 4;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), textX, textY, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    public static void drawRoundedBox(Matrix4f matrix4f, float x, float y, float width, float height, float radius, Color color) {

        Render2D.drawRoundedBoxOutline(matrix4f, x, y, width, height, radius, color);

    }
}