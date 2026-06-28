package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import com.withouthonor.npcs.network.ProfileSharePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class ClientNetHandlers {

    private ClientNetHandlers() {
    }

    public static void openSpawner(BlockPos pos, SpawnerBlockEntity.Config config, List<String> available) {
        Minecraft.getInstance().setScreen(
                new com.withouthonor.npcs.client.gui.editor.SpawnerEditScreen(pos, config, available));
    }

    public static void openImport(List<ProfileSharePackets.FileEntry> files, boolean forSpawn) {
        Minecraft mc = Minecraft.getInstance();
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
