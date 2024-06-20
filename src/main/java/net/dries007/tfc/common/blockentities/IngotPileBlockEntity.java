/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;

public class IngotPileBlockEntity extends TFCBlockEntity
{
    private final List<Entry> entries;

    public IngotPileBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.INGOT_PILE.get(), pos, state);

        entries = new ArrayList<>();
    }

    public void addIngot(ItemStack stack)
    {
        entries.add(new Entry(stack));
        markForSync();
    }

    public void removeAllIngots(Consumer<ItemStack> ingotConsumer)
    {
        for (Entry entry : this.entries)
        {
            ingotConsumer.accept(entry.stack);
        }
        this.entries.clear();
        markForSync();
    }

    public ItemStack removeIngot()
    {
        if (!entries.isEmpty())
        {
            final Entry entry = entries.remove(entries.size() - 1);
            markForSync();
            return entry.stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns a cached metal for the given side, if present, otherwise grabs from the cache.
     * The metal is defined by checking what metal the stack would melt into if heated.
     * Any other items turn into {@link Metal#unknown()}.
     */
    public Metal getOrCacheMetal(int index)
    {
        if (index >= entries.size())
        {
            return Metal.unknown();
        }

        final Entry entry;
        try
        {
            entry = entries.get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            // This is terrible, but it's a threadsafety issue. `entries` might be updated between the bounds check above, and this query
            return Metal.unknown();
        }

        if (entry.metal == null)
        {
            entry.metal = Metal.getFromIngot(entry.stack);
            if (entry.metal == null)
            {
                entry.metal = Metal.unknown();
            }
        }
        return entry.metal;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        final ListTag stacks = new ListTag();
        for (final Entry entry : entries)
        {
            stacks.add(entry.stack.save(new CompoundTag()));
        }
        tag.put("stacks", stacks);
        super.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag)
    {
        entries.clear();
        final ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            entries.add(new Entry(ItemStack.of(list.getCompound(i))));
        }
        super.loadAdditional(tag);
    }

    public void fillTooltip(Consumer<Component> tooltip)
    {
        final Object2IntMap<Metal> map = new Object2IntOpenHashMap<>();
        for (Entry entry : entries)
        {
            final Metal metal = entry.metal;
            if (metal != null)
            {
                map.mergeInt(metal, 1, Integer::sum);
            }
        }
        map.forEach((metal, ct) -> tooltip.accept(Component.literal("" + ct + "x ").append(metal.getDisplayName())));
    }

    public ItemStack getPickedItemStack()
    {
        return entries.isEmpty() ? ItemStack.EMPTY : entries.get(0).stack.copy();
    }

    static class Entry
    {
        final ItemStack stack;
        @Nullable Metal metal;

        Entry(ItemStack stack)
        {
            this.stack = stack;
        }
    }
}
