package com.withouthonor.npcs.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class SelectableEditBox extends EditBox {

    private static final long DOUBLE_CLICK_MS = 250;

    private int dragAnchor = -1;
    private long lastClickTime;

    private String valueOnFocus = "";

    public SelectableEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void setHint(Component hint) {
        super.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < DOUBLE_CLICK_MS) {

            moveCursorToEnd();
            setHighlightPos(0);
            dragAnchor = -1;
        } else {
            super.onClick(mouseX, mouseY);
            dragAnchor = getCursorPosition();
        }
        lastClickTime = now;
    }

    @Override
    public void setFocused(boolean focused) {
        boolean wasFocused = isFocused();
        super.setFocused(focused);
        if (focused && !wasFocused) {
            valueOnFocus = getValue();
        }
        if (!focused) {

            setHighlightPos(getCursorPosition());
            dragAnchor = -1;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (isFocused() && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_Z
                && (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0) {
            setValue(valueOnFocus);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(mouseX, mouseY, dragX, dragY);
        if (dragAnchor >= 0) {

            super.onClick(mouseX, mouseY);
            setHighlightPos(dragAnchor);
        }
    }
}
