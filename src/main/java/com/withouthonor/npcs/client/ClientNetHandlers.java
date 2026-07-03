package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import com.withouthonor.npcs.network.NpcListPackets;
import com.withouthonor.npcs.network.ProfileSharePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class ClientNetHandlers {

    private ClientNetHandlers() {
    }

    /** Одноразовый колбэк «выбора файла экспорта»: если задан, следующий пришедший
     *  список файлов откроется в режиме выбора (действие «Преображение»). */
    @javax.annotation.Nullable
    public static java.util.function.Consumer<String> filePickCallback;

    /** Одноразовый колбэк «выбора NPC»: если задан, следующий пришедший список NPC
     *  откроется в пикере (привязка NPC к блок-триггеру и т.п.). */
    @javax.annotation.Nullable
    public static java.util.function.Consumer<NpcListPackets.NpcEntry> npcPickCallback;

    public static void openSpawner(BlockPos pos, SpawnerBlockEntity.Config config, List<String> available) {
        Minecraft.getInstance().setScreen(
                new com.withouthonor.npcs.client.gui.editor.SpawnerEditScreen(pos, config, available));
    }

    public static void openNpcPick(List<NpcListPackets.NpcEntry> entries) {
        Minecraft mc = Minecraft.getInstance();
        java.util.function.Consumer<NpcListPackets.NpcEntry> pick = npcPickCallback;
        if (pick == null) {
            // список никто не ждёт — игнорируем
            return;
        }
        npcPickCallback = null;
        mc.setScreen(new com.withouthonor.npcs.client.gui.editor.NpcPickScreen(mc.screen, entries, pick));
    }

    public static void openImport(List<ProfileSharePackets.FileEntry> files, boolean forSpawn) {
        Minecraft mc = Minecraft.getInstance();
        java.util.function.Consumer<String> pick = filePickCallback;
        if (pick != null) {
            filePickCallback = null;
            mc.setScreen(new com.withouthonor.npcs.client.gui.editor.ImportPickerScreen(
                    mc.screen, files, pick));
            return;
        }
        if (forSpawn) {
            mc.setScreen(new com.withouthonor.npcs.client.gui.editor.ImportPickerScreen(files));
        } else if (mc.screen instanceof com.withouthonor.npcs.client.gui.editor.SecondCharPickerScreen sc) {
            sc.acceptList(files);
        } else if (mc.screen instanceof com.withouthonor.npcs.client.gui.editor.ImportPickerScreen picker) {
            picker.acceptList(files);
        } else if (mc.screen instanceof com.withouthonor.npcs.client.gui.editor.NpcEditorScreen editor
                && editor.editedEntityId() != -1) {
            mc.setScreen(new com.withouthonor.npcs.client.gui.editor.ImportPickerScreen(
                    editor, editor.editedEntityId(), files));
        }
    }
}
