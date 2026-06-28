package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.block.TriggerBlockEntity;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.TriggerSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

public class TriggerEditorScreen extends ScaledScreen {

    private static final int WIN_W = 280;
    private static final int WIN_H = 196;

    private final BlockPos pos;
    private boolean once;
    private final List<DialogueAction> actions;
    private final List<DialogueCondition> conditions;
    private final boolean viaHelper;
    @Nullable
    private Direction enterDir;

    private int winX, winY, winW, winH;

    private TriggerEditorScreen(BlockPos pos, boolean once, List<DialogueAction> actions,
                                List<DialogueCondition> conditions, boolean viaHelper,
                                @Nullable Direction enterDir) {
        super(Component.translatable("wh_npcs.ui.trigger_edit.title"));
        this.pos = pos;
        this.once = once;
        this.actions = actions;
        this.conditions = conditions;
        this.viaHelper = viaHelper;
        this.enterDir = enterDir;
    }

    public static void open(BlockPos pos, boolean once, String actionsJson, String conditionsJson,
                            boolean viaHelper, byte enterDir) {
        Minecraft.getInstance().setScreen(new TriggerEditorScreen(pos, once,
                TriggerBlockEntity.actionsFromJson(actionsJson),
                TriggerBlockEntity.conditionsFromJson(conditionsJson), viaHelper,
                TriggerBlockEntity.dirFromByte(enterDir)));
    }

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return WIN_H;
    }

    private void recalc() {
        winW = WIN_W;
        winH = WIN_H;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
    }

    @Override
    protected void init() {
        recalc();
    }

    private int onceY() {
        return winY + 42;
    }

    private int dirY() {
        return winY + 72;
    }

    private int condY() {
        return winY + 98;
    }

    private int actY() {
        return winY + 120;
    }

    private int footY() {
        return winY + winH - 26;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.drawString(font, Component.translatable("wh_npcs.ui.trigger_edit.header").getString(), winX + 10, winY + 10, VanillaUIHelper.TEXT_WHITE, false);
        g.drawString(font, viaHelper
                        ? Component.translatable("wh_npcs.ui.trigger_edit.via_helper", pos.getX() + " " + pos.getY() + " " + pos.getZ()).getString()
                        : Component.translatable("wh_npcs.ui.trigger_edit.desc").getString(),
                winX + 10, winY + 24, VanillaUIHelper.TEXT_WHITE, false);

        int cy = onceY();
        boolean onceHover = isOver(mouseX, mouseY, winX + 10, cy, 220, 12);
        VanillaUIHelper.drawButton(g, winX + 10, cy, 12, 12, onceHover);
        if (once) {
            g.drawCenteredString(font, "§a✓", winX + 16, cy + 2, VanillaUIHelper.TEXT_WHITE);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.trigger_edit.once").getString(), winX + 28, cy + 2,
                once ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, once ? Component.translatable("wh_npcs.ui.trigger_edit.once_on").getString()
                : Component.translatable("wh_npcs.ui.trigger_edit.once_off").getString(), winX + 28, cy + 14, VanillaUIHelper.TEXT_WHITE, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.trigger_edit.enter_dir").getString(), winX + 10, dirY() + 5, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSmallButton(g, font, dirLabel(enterDir), winX + 130, dirY(), winW - 140,
                isOver(mouseX, mouseY, winX + 130, dirY(), winW - 140, 18),
                enterDir == null ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_GOLD);

        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.trigger_edit.conditions", conditions.size()).getString() + "  →",
                winX + 10, condY(), winW - 20, isOver(mouseX, mouseY, winX + 10, condY(), winW - 20, 18),
                VanillaUIHelper.TEXT_GOLD);
        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.trigger_edit.actions", actions.size()).getString() + "  →",
                winX + 10, actY(), winW - 20, isOver(mouseX, mouseY, winX + 10, actY(), winW - 20, 18),
                VanillaUIHelper.TEXT_AQUA);

        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.common.save").getString(), winX + 10, footY(), 100,
                isOver(mouseX, mouseY, winX + 10, footY(), 100, 18), VanillaUIHelper.TEXT_GREEN);
        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.common.close").getString(), winX + winW - 80, footY(), 70,
                isOver(mouseX, mouseY, winX + winW - 80, footY(), 70, 18), VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOver(mouseX, mouseY, winX + 10, onceY(), 220, 12)) {
                once = !once;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + 130, dirY(), winW - 140, 18)) {
                enterDir = cycleDir(enterDir);
                return true;
            }
            if (isOver(mouseX, mouseY, winX + 10, condY(), winW - 20, 18)) {
                ConditionsEditorScreen.openForConditions(this, conditions);
                return true;
            }
            if (isOver(mouseX, mouseY, winX + 10, actY(), winW - 20, 18)) {
                ActionsEditorScreen.open(this, actions);
                return true;
            }
            if (isOver(mouseX, mouseY, winX + 10, footY(), 100, 18)) {
                NetworkHandler.sendToServer(new TriggerSavePacket(pos, once,
                        TriggerBlockEntity.actionsToJson(actions),
                        TriggerBlockEntity.conditionsToJson(conditions),
                        TriggerBlockEntity.dirToByte(enterDir)));
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - 80, footY(), 70, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private static String dirLabel(@Nullable Direction d) {
        if (d == null) {
            return Component.translatable("wh_npcs.ui.trigger_edit.dir_any").getString();
        }
        return switch (d) {
            case NORTH -> Component.translatable("wh_npcs.ui.trigger_edit.dir_north").getString();
            case SOUTH -> Component.translatable("wh_npcs.ui.trigger_edit.dir_south").getString();
            case WEST -> Component.translatable("wh_npcs.ui.trigger_edit.dir_west").getString();
            case EAST -> Component.translatable("wh_npcs.ui.trigger_edit.dir_east").getString();
            default -> Component.translatable("wh_npcs.ui.trigger_edit.dir_any").getString();
        };
    }

    @Nullable
    private static Direction cycleDir(@Nullable Direction d) {
        if (d == null) {
            return Direction.NORTH;
        }
        return switch (d) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            default -> null;
        };
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
