package com.yourname.floptext_rgb.screen;

import com.yourname.floptext_rgb.ui.FlopTheme;

import com.yourname.floptext_rgb.compat.Compat;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.yourname.floptext_rgb.math.GradientCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Screen-sampling color picker reconstructed from the original v1.7.0 behavior.
 * Captures the current framebuffer once, then maps GUI coordinates to framebuffer
 * pixels so a left click applies the color from the game scene under the pointer.
 */
public final class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer onPicked;
    private volatile int[] snapshot;
    private volatile int snapshotWidth;
    private volatile int snapshotHeight;
    private boolean captureRequested;
    private volatile boolean captureFailed;

    public ColorPickerScreen(Screen parent, IntConsumer onPicked) {
        super(Component.literal("FlopText RGB Color Picker"));
        this.parent = parent;
        this.onPicked = onPicked;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!captureRequested) {
            captureRequested = true;
            requestCapture();
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        FlopTheme.paintButtons(context, font, this, mouseX, mouseY);

        String hint = captureFailed
            ? "Screen capture failed - press Esc"
            : snapshot == null
                ? "Capturing screen...  |  Esc to cancel"
                : "Click to pick a color  |  Esc to cancel";
        int hintWidth = font.width(hint);
        int hintX = (width - hintWidth) / 2;
        context.fill(hintX - 6, 4, hintX + hintWidth + 6, 22, 0xCC101010);
        context.text(font, Component.literal(hint), hintX, 8, 0xFFFFFFFF);

        int sampled = sample(mouseX, mouseY);
        if (sampled >= 0) {
            String hex = GradientCalculator.toDisplayHex(sampled);
            int boxWidth = 18 + font.width(hex) + 10;
            int boxX = Math.min(mouseX + 12, Math.max(2, width - boxWidth - 2));
            int boxY = Math.min(mouseY + 12, Math.max(2, height - 24));
            context.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + 20, 0xFFFFFFFF);
            context.fill(boxX, boxY, boxX + boxWidth, boxY + 19, 0xFF000000);
            context.fill(boxX + 2, boxY + 2, boxX + 18, boxY + 18, 0xFF000000 | sampled);
            context.text(font, Component.literal(hex), boxX + 22, boxY + 5, 0xFFFFFFFF);
        }
    }

    private void requestCapture() {
        try {
            Minecraft client = Minecraft.getInstance();
            RenderTarget target = Compat.mainRenderTarget();
            if (target == null) { captureFailed = true; return; }
            Screenshot.takeScreenshot(target, image -> {
                try {
                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w <= 0 || h <= 0 || (long) w * h > 100_000_000L) {
                        captureFailed = true;
                        return;
                    }
                    int[] pixels = new int[w * h];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            pixels[y * w + x] = image.getPixel(x, y) & 0xFFFFFF;
                        }
                    }
                    snapshotWidth = w;
                    snapshotHeight = h;
                    snapshot = pixels;
                } catch (RuntimeException exception) {
                    captureFailed = true;
                } finally {
                    image.close();
                }
            });
        } catch (RuntimeException exception) {
            captureFailed = true;
        }
    }

    private int sample(double guiX, double guiY) {
        int[] pixels = snapshot;
        int w = snapshotWidth;
        int h = snapshotHeight;
        if (pixels == null || w <= 0 || h <= 0 || width <= 0 || height <= 0) return -1;
        int x = (int) Math.floor(guiX * w / width);
        int y = (int) Math.floor(guiY * h / height);
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        int index = y * w + x;
        return index >= 0 && index < pixels.length ? pixels[index] : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (Compat.isLeft(click.button())) {
            int rgb = sample(click.x(), click.y());
            if (rgb >= 0) {
                onPicked.accept(rgb);
                Compat.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        Compat.setScreen(parent);
    }
}
