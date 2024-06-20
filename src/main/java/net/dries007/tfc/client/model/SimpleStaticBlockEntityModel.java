/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.model;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;

public interface SimpleStaticBlockEntityModel<T extends IBakedGeometry<T>, B extends BlockEntity> extends IBakedGeometry<T>, IStaticBakedModel
{
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    default ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData)
    {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.getType() == type())
        {
            return modelData.derive()
                .with(StaticModelData.PROPERTY, render(level, pos, (B) blockEntity))
                .build();
        }
        return modelData;
    }

    default StaticModelData render(BlockAndTintGetter level, BlockPos pos, B blockEntity)
    {
        final int packedLight = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
        final int packedOverlay = OverlayTexture.NO_OVERLAY;
        final List<BakedQuad> quads = new ArrayList<>(faces(blockEntity));
        final VertexConsumer buffer = new QuadBakingVertexConsumer(quads::add);
        final PoseStack poseStack = new PoseStack();

        final TextureAtlasSprite particle = render(blockEntity, poseStack, buffer, packedLight, packedOverlay);
        return new StaticModelData(quads, particle);
    }

    /**
     * @return {@link TextureAtlasSprite a particle texture}
     */
    TextureAtlasSprite render(B blockEntity, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay);

    BlockEntityType<B> type();

    /**
     * @return An estimate for the number of {@link BakedQuad}s to be created, for capacity-allocation.
     */
    int faces(B blockEntity);
}
