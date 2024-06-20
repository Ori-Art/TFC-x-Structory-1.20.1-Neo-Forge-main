/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.util.Helpers;
import org.jetbrains.annotations.Nullable;

public class CopyFluidFunction extends LootItemConditionalFunction
{
    /**
     * Copies the fluid contained in {@code entity} into the {@code stack}. This does not mutate the block entity's content.
     * @return The {@code stack} but with the fluid contained within the block entity
     */
    public static ItemStack copyToItem(ItemStack stack, @Nullable BlockEntity entity)
    {
        if (entity != null && !stack.isEmpty())
        {
            final IFluidHandlerItem itemHandler = Helpers.getCapability(stack, Capabilities.FLUID_ITEM);
            final IFluidHandler blockHandler = Helpers.getCapability(entity, Capabilities.FLUID);
            if (itemHandler != null && blockHandler != null)
            {
                itemHandler.fill(blockHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.EXECUTE);
                return itemHandler.getContainer();
            }
        }
        return stack;
    }

    /**
     * Copies the fluid contained in {@code stack} into the {@code entity}. This does not modify the stack, only the block entity.
     */
    public static void copyFromItem(ItemStack stack, @Nullable BlockEntity entity)
    {
        if (entity != null && !stack.isEmpty())
        {
            final IFluidHandlerItem itemHandler = Helpers.getCapability(stack, Capabilities.FLUID_ITEM);
            final IFluidHandler blockHandler = Helpers.getCapability(entity, Capabilities.FLUID);
            if (itemHandler != null && blockHandler != null)
            {
                blockHandler.fill(itemHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    public CopyFluidFunction(LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @Override
    public LootItemFunctionType getType()
    {
        return TFCLoot.COPY_FLUID.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context)
    {
        if (context.hasParam(LootContextParams.BLOCK_ENTITY))
        {
            return copyToItem(stack, context.getParam(LootContextParams.BLOCK_ENTITY));
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyFluidFunction>
    {
        @Override
        public CopyFluidFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions)
        {
            return new CopyFluidFunction(conditions);
        }
    }
}
