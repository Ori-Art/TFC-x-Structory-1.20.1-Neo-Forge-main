/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Like {@link TickableInventoryBlockEntity} for blocks without an inventory
 */
public abstract class TickableBlockEntity extends TFCBlockEntity
{
    protected boolean needsClientUpdate;
    protected boolean isDirty;

    protected TickableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void checkForLastTickSync()
    {
        if (needsClientUpdate)
        {
            // only sync further down when we actually request it to be synced
            needsClientUpdate = false;
            super.markForSync();
        }
        if (isDirty)
        {
            isDirty = false;
            super.markDirty();
        }
    }

    @Override
    public void markForSync()
    {
        needsClientUpdate = true;
    }

    @Override
    public void markDirty()
    {
        isDirty = true;
    }
}
