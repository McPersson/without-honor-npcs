package com.withouthonor.npcs.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class CuriosNpcLayer extends RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>> {

    private final RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> parent;
    private final java.util.Map<Item, Optional<ICurioRenderer>> rendererCache = new java.util.HashMap<>();

    public CuriosNpcLayer(RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> parent) {
        super(parent);
        this.parent = parent;
    }

    @Nullable
    private net.minecraft.client.model.geom.ModelPart attachPart(String slotType) {
        PlayerModel<CompanionEntity> model = parent.getModel();
        return switch (slotType) {
            case "head" -> model.head;
            case "body", "back", "necklace" -> model.body;
            default -> null;
        };
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, CompanionEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getCurios().forEach((id, stacksHandler) -> {
                    IDynamicStackHandler stacks = stacksHandler.getStacks();
                    NonNullList<Boolean> renders = stacksHandler.getRenders();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        boolean show = i >= renders.size() || renders.get(i);
                        if (stack.isEmpty() || !show) {
                            continue;
                        }
                        Optional<ICurioRenderer> renderer =
                                rendererCache.computeIfAbsent(stack.getItem(), CuriosRendererRegistry::getRenderer);
                        if (renderer.isPresent()) {
                            SlotContext ctx = new SlotContext(id, entity, i, false, show);

                            net.minecraft.client.model.geom.ModelPart part = attachPart(id);
                            if (part != null) {
                                pose.pushPose();
                                part.translateAndRotate(pose);
                                renderer.get().render(stack, ctx, pose, this.parent, buffer, light,
                                        limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                                pose.popPose();
                            } else {
                                renderer.get().render(stack, ctx, pose, this.parent, buffer, light,
                                        limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                            }
                        }
                    }
                }));
    }
}
