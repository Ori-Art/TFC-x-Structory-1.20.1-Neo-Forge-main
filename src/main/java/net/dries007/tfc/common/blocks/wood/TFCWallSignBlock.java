/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;

import org.jetbrains.annotations.Nullable;

public class TFCWallSignBlock extends WallSignBlock implements IForgeBlockExtension, EntityBlockExtension
{
    private final ExtendedProperties properties;

    public TFCWallSignBlock(ExtendedProperties properties, WoodType type)
    {
        super(properties.properties(), type);
        this.properties = properties;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return EntityBlockExtension.super.getTicker(level, state, type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return EntityBlockExtension.super.newBlockEntity(pos, state);
    }

    @Override
    public ExtendedProperties getExtendedProperties()
    {
        return properties;
    }
}
