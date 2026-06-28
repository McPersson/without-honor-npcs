package com.withouthonor.npcs.network;

import com.withouthonor.npcs.WHCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(WHCompanions.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(RequestSkinPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestSkinPacket::encode)
                .decoder(RequestSkinPacket::decode)
                .consumerMainThread(RequestSkinPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkinDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkinDataPacket::encode)
                .decoder(SkinDataPacket::decode)
                .consumerMainThread(SkinDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenDialogueNodePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialogueNodePacket::encode)
                .decoder(OpenDialogueNodePacket::decode)
                .consumerMainThread(OpenDialogueNodePacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueChoicePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueChoicePacket::encode)
                .decoder(DialogueChoicePacket::decode)
                .consumerMainThread(DialogueChoicePacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueClosePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueClosePacket::encode)
                .decoder(DialogueClosePacket::decode)
                .consumerMainThread(DialogueClosePacket::handle)
                .add();

        CHANNEL.messageBuilder(EditorDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EditorDataPacket::encode)
                .decoder(EditorDataPacket::decode)
                .consumerMainThread(EditorDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveProfilePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveProfilePacket::encode)
                .decoder(SaveProfilePacket::decode)
                .consumerMainThread(SaveProfilePacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestDialoguePacket::encode)
                .decoder(RequestDialoguePacket::decode)
                .consumerMainThread(RequestDialoguePacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueDataPacket::encode)
                .decoder(DialogueDataPacket::decode)
                .consumerMainThread(DialogueDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveDialoguePacket::encode)
                .decoder(SaveDialoguePacket::decode)
                .consumerMainThread(SaveDialoguePacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestImagePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestImagePacket::encode)
                .decoder(RequestImagePacket::decode)
                .consumerMainThread(RequestImagePacket::handle)
                .add();

        CHANNEL.messageBuilder(ImageChunkPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImageChunkPacket::encode)
                .decoder(ImageChunkPacket::decode)
                .consumerMainThread(ImageChunkPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestImageListPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestImageListPacket::encode)
                .decoder(RequestImageListPacket::decode)
                .consumerMainThread(RequestImageListPacket::handle)
                .add();

        CHANNEL.messageBuilder(ImageListPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImageListPacket::encode)
                .decoder(ImageListPacket::decode)
                .consumerMainThread(ImageListPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteImagePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteImagePacket::encode)
                .decoder(DeleteImagePacket::decode)
                .consumerMainThread(DeleteImagePacket::handle)
                .add();

        CHANNEL.messageBuilder(RenameImagePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameImagePacket::encode)
                .decoder(RenameImagePacket::decode)
                .consumerMainThread(RenameImagePacket::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.Request.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinLibraryPackets.Request::encode)
                .decoder(SkinLibraryPackets.Request::decode)
                .consumerMainThread(SkinLibraryPackets.Request::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.Library.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkinLibraryPackets.Library::encode)
                .decoder(SkinLibraryPackets.Library::decode)
                .consumerMainThread(SkinLibraryPackets.Library::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.AddUrl.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinLibraryPackets.AddUrl::encode)
                .decoder(SkinLibraryPackets.AddUrl::decode)
                .consumerMainThread(SkinLibraryPackets.AddUrl::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.Upload.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinLibraryPackets.Upload::encode)
                .decoder(SkinLibraryPackets.Upload::decode)
                .consumerMainThread(SkinLibraryPackets.Upload::handle)
                .add();

        CHANNEL.messageBuilder(CuriosPackets.Request.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CuriosPackets.Request::encode)
                .decoder(CuriosPackets.Request::decode)
                .consumerMainThread(CuriosPackets.Request::handle)
                .add();

        CHANNEL.messageBuilder(CuriosPackets.ListResult.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CuriosPackets.ListResult::encode)
                .decoder(CuriosPackets.ListResult::decode)
                .consumerMainThread(CuriosPackets.ListResult::handle)
                .add();

        CHANNEL.messageBuilder(CuriosPackets.Save.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CuriosPackets.Save::encode)
                .decoder(CuriosPackets.Save::decode)
                .consumerMainThread(CuriosPackets.Save::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.Delete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinLibraryPackets.Delete::encode)
                .decoder(SkinLibraryPackets.Delete::decode)
                .consumerMainThread(SkinLibraryPackets.Delete::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.Request.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FactionPackets.Request::encode)
                .decoder(FactionPackets.Request::decode)
                .consumerMainThread(FactionPackets.Request::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.Data.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FactionPackets.Data::encode)
                .decoder(FactionPackets.Data::decode)
                .consumerMainThread(FactionPackets.Data::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.Save.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FactionPackets.Save::encode)
                .decoder(FactionPackets.Save::decode)
                .consumerMainThread(FactionPackets.Save::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.RepRequest.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FactionPackets.RepRequest::encode)
                .decoder(FactionPackets.RepRequest::decode)
                .consumerMainThread(FactionPackets.RepRequest::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.RepData.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FactionPackets.RepData::encode)
                .decoder(FactionPackets.RepData::decode)
                .consumerMainThread(FactionPackets.RepData::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.RepSet.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FactionPackets.RepSet::encode)
                .decoder(FactionPackets.RepSet::decode)
                .consumerMainThread(FactionPackets.RepSet::handle)
                .add();

        CHANNEL.messageBuilder(FactionPackets.Delete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FactionPackets.Delete::encode)
                .decoder(FactionPackets.Delete::decode)
                .consumerMainThread(FactionPackets.Delete::handle)
                .add();

        CHANNEL.messageBuilder(DeleteDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteDialoguePacket::encode)
                .decoder(DeleteDialoguePacket::decode)
                .consumerMainThread(DeleteDialoguePacket::handle)
                .add();

        CHANNEL.messageBuilder(EditorMoveItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EditorMoveItemPacket::encode)
                .decoder(EditorMoveItemPacket::decode)
                .consumerMainThread(EditorMoveItemPacket::handle)
                .add();

        CHANNEL.messageBuilder(SpeechBubblePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpeechBubblePacket::encode)
                .decoder(SpeechBubblePacket::decode)
                .consumerMainThread(SpeechBubblePacket::handle)
                .add();

        CHANNEL.messageBuilder(EmotePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmotePacket::encode)
                .decoder(EmotePacket::decode)
                .consumerMainThread(EmotePacket::handle)
                .add();

        CHANNEL.messageBuilder(EmotecraftEmotePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmotecraftEmotePacket::encode)
                .decoder(EmotecraftEmotePacket::decode)
                .consumerMainThread(EmotecraftEmotePacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteCompanionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteCompanionPacket::encode)
                .decoder(DeleteCompanionPacket::decode)
                .consumerMainThread(DeleteCompanionPacket::handle)
                .add();

        CHANNEL.messageBuilder(IndicatorPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(IndicatorPacket::encode)
                .decoder(IndicatorPacket::decode)
                .consumerMainThread(IndicatorPacket::handle)
                .add();

        CHANNEL.messageBuilder(GlossaryPackets.Request.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GlossaryPackets.Request::encode)
                .decoder(GlossaryPackets.Request::decode)
                .consumerMainThread(GlossaryPackets.Request::handle)
                .add();

        CHANNEL.messageBuilder(GlossaryPackets.Data.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GlossaryPackets.Data::encode)
                .decoder(GlossaryPackets.Data::decode)
                .consumerMainThread(GlossaryPackets.Data::handle)
                .add();

        CHANNEL.messageBuilder(GlossaryPackets.Save.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GlossaryPackets.Save::encode)
                .decoder(GlossaryPackets.Save::decode)
                .consumerMainThread(GlossaryPackets.Save::handle)
                .add();

        CHANNEL.messageBuilder(GlossaryPackets.Delete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GlossaryPackets.Delete::encode)
                .decoder(GlossaryPackets.Delete::decode)
                .consumerMainThread(GlossaryPackets.Delete::handle)
                .add();

        CHANNEL.messageBuilder(MonologuePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MonologuePacket::encode)
                .decoder(MonologuePacket::decode)
                .consumerMainThread(MonologuePacket::handle)
                .add();

        CHANNEL.messageBuilder(RestockTradesPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RestockTradesPacket::encode)
                .decoder(RestockTradesPacket::decode)
                .consumerMainThread(RestockTradesPacket::handle)
                .add();

        CHANNEL.messageBuilder(EditorGiveItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EditorGiveItemPacket::encode)
                .decoder(EditorGiveItemPacket::decode)
                .consumerMainThread(EditorGiveItemPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveEquipmentPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveEquipmentPacket::encode)
                .decoder(SaveEquipmentPacket::decode)
                .consumerMainThread(SaveEquipmentPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkinLibraryPackets.Rename.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinLibraryPackets.Rename::encode)
                .decoder(SkinLibraryPackets.Rename::decode)
                .consumerMainThread(SkinLibraryPackets.Rename::handle)
                .add();

        CHANNEL.messageBuilder(TriggerEditPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TriggerEditPacket::encode)
                .decoder(TriggerEditPacket::decode)
                .consumerMainThread(TriggerEditPacket::handle)
                .add();

        CHANNEL.messageBuilder(TriggerSavePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TriggerSavePacket::encode)
                .decoder(TriggerSavePacket::decode)
                .consumerMainThread(TriggerSavePacket::handle)
                .add();

        CHANNEL.messageBuilder(StopMusicPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StopMusicPacket::encode)
                .decoder(StopMusicPacket::decode)
                .consumerMainThread(StopMusicPacket::handle)
                .add();

        CHANNEL.messageBuilder(FollowControlPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FollowControlPacket::encode)
                .decoder(FollowControlPacket::decode)
                .consumerMainThread(FollowControlPacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueInputPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueInputPacket::encode)
                .decoder(DialogueInputPacket::decode)
                .consumerMainThread(DialogueInputPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.Export.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.Export::encode)
                .decoder(ProfileSharePackets.Export::decode)
                .consumerMainThread(ProfileSharePackets.Export::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.RequestList.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.RequestList::encode)
                .decoder(ProfileSharePackets.RequestList::decode)
                .consumerMainThread(ProfileSharePackets.RequestList::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.ListResult.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfileSharePackets.ListResult::encode)
                .decoder(ProfileSharePackets.ListResult::decode)
                .consumerMainThread(ProfileSharePackets.ListResult::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.Import.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.Import::encode)
                .decoder(ProfileSharePackets.Import::decode)
                .consumerMainThread(ProfileSharePackets.Import::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.Delete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.Delete::encode)
                .decoder(ProfileSharePackets.Delete::decode)
                .consumerMainThread(ProfileSharePackets.Delete::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.Rename.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.Rename::encode)
                .decoder(ProfileSharePackets.Rename::decode)
                .consumerMainThread(ProfileSharePackets.Rename::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.BookRequest.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.BookRequest::encode)
                .decoder(ProfileSharePackets.BookRequest::decode)
                .consumerMainThread(ProfileSharePackets.BookRequest::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.SpawnFromFile.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.SpawnFromFile::encode)
                .decoder(ProfileSharePackets.SpawnFromFile::decode)
                .consumerMainThread(ProfileSharePackets.SpawnFromFile::handle)
                .add();

        CHANNEL.messageBuilder(ProfileSharePackets.SpawnFromClient.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileSharePackets.SpawnFromClient::encode)
                .decoder(ProfileSharePackets.SpawnFromClient::decode)
                .consumerMainThread(ProfileSharePackets.SpawnFromClient::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.RequestPlayers.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlagPackets.RequestPlayers::encode)
                .decoder(FlagPackets.RequestPlayers::decode)
                .consumerMainThread(FlagPackets.RequestPlayers::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.PlayersResult.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FlagPackets.PlayersResult::encode)
                .decoder(FlagPackets.PlayersResult::decode)
                .consumerMainThread(FlagPackets.PlayersResult::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.RequestFlags.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlagPackets.RequestFlags::encode)
                .decoder(FlagPackets.RequestFlags::decode)
                .consumerMainThread(FlagPackets.RequestFlags::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.FlagsResult.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FlagPackets.FlagsResult::encode)
                .decoder(FlagPackets.FlagsResult::decode)
                .consumerMainThread(FlagPackets.FlagsResult::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.SetDescription.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlagPackets.SetDescription::encode)
                .decoder(FlagPackets.SetDescription::decode)
                .consumerMainThread(FlagPackets.SetDescription::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.RemoveFlag.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlagPackets.RemoveFlag::encode)
                .decoder(FlagPackets.RemoveFlag::decode)
                .consumerMainThread(FlagPackets.RemoveFlag::handle)
                .add();

        CHANNEL.messageBuilder(FlagPackets.AddFlag.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlagPackets.AddFlag::encode)
                .decoder(FlagPackets.AddFlag::decode)
                .consumerMainThread(FlagPackets.AddFlag::handle)
                .add();

        CHANNEL.messageBuilder(SpawnerPackets.OpenEditor.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnerPackets.OpenEditor::encode)
                .decoder(SpawnerPackets.OpenEditor::decode)
                .consumerMainThread(SpawnerPackets.OpenEditor::handle)
                .add();

        CHANNEL.messageBuilder(SpawnerPackets.Save.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpawnerPackets.Save::encode)
                .decoder(SpawnerPackets.Save::decode)
                .consumerMainThread(SpawnerPackets.Save::handle)
                .add();

        CHANNEL.messageBuilder(NpcMoverPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NpcMoverPacket::encode)
                .decoder(NpcMoverPacket::decode)
                .consumerMainThread(NpcMoverPacket::handle)
                .add();

        CHANNEL.messageBuilder(PoseLibraryPackets.Save.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PoseLibraryPackets.Save::encode)
                .decoder(PoseLibraryPackets.Save::decode)
                .consumerMainThread(PoseLibraryPackets.Save::handle)
                .add();

        CHANNEL.messageBuilder(PoseLibraryPackets.RequestList.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PoseLibraryPackets.RequestList::encode)
                .decoder(PoseLibraryPackets.RequestList::decode)
                .consumerMainThread(PoseLibraryPackets.RequestList::handle)
                .add();

        CHANNEL.messageBuilder(PoseLibraryPackets.ListResult.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PoseLibraryPackets.ListResult::encode)
                .decoder(PoseLibraryPackets.ListResult::decode)
                .consumerMainThread(PoseLibraryPackets.ListResult::handle)
                .add();

        CHANNEL.messageBuilder(PoseLibraryPackets.Delete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PoseLibraryPackets.Delete::encode)
                .decoder(PoseLibraryPackets.Delete::decode)
                .consumerMainThread(PoseLibraryPackets.Delete::handle)
                .add();

        CHANNEL.messageBuilder(RefreshDialoguesPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RefreshDialoguesPacket::encode)
                .decoder(RefreshDialoguesPacket::decode)
                .consumerMainThread(RefreshDialoguesPacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueListPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueListPacket::encode)
                .decoder(DialogueListPacket::decode)
                .consumerMainThread(DialogueListPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClientImportPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClientImportPacket::encode)
                .decoder(ClientImportPacket::decode)
                .consumerMainThread(ClientImportPacket::handle)
                .add();

        CHANNEL.messageBuilder(ImageUploadPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ImageUploadPacket::encode)
                .decoder(ImageUploadPacket::decode)
                .consumerMainThread(ImageUploadPacket::handle)
                .add();

        CHANNEL.messageBuilder(ImageUploadResultPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImageUploadResultPacket::encode)
                .decoder(ImageUploadResultPacket::decode)
                .consumerMainThread(ImageUploadResultPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestDialogueBundle.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestDialogueBundle::encode)
                .decoder(RequestDialogueBundle::decode)
                .consumerMainThread(RequestDialogueBundle::handle)
                .add();

        CHANNEL.messageBuilder(DialogueBundlePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueBundlePacket::encode)
                .decoder(DialogueBundlePacket::decode)
                .consumerMainThread(DialogueBundlePacket::handle)
                .add();

        CHANNEL.messageBuilder(NpcAdminActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NpcAdminActionPacket::encode)
                .decoder(NpcAdminActionPacket::decode)
                .consumerMainThread(NpcAdminActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(RenameDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameDialoguePacket::encode)
                .decoder(RenameDialoguePacket::decode)
                .consumerMainThread(RenameDialoguePacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
