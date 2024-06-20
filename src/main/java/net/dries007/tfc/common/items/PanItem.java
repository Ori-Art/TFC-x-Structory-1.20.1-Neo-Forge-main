/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.items;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.client.render.blockentity.PanItemRenderer;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Pannable;

public class PanItem extends Item
{
    public static final int USE_TIME = 120;

    @Nullable
    public static BlockState readState(HolderGetter<Block> getter, ItemStack stack)
    {
        final CompoundTag tag = stack.getTagElement("state");
        if (tag != null)
        {
            return NbtUtils.readBlockState(getter, tag);
        }
        return null;
    }

    @Nullable
    public static Pannable readPannable(HolderGetter<Block> getter, ItemStack stack)
    {
        final BlockState state = readState(getter, stack);
        if (state != null)
        {
            return Pannable.get(state);
        }
        return null;
    }

    public PanItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return USE_TIME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (hand == InteractionHand.OFF_HAND)
        {
            // We require pans be operated with the main hand - as that's when they render as two-handed
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (Helpers.isFluid(level.getFluidState(player.blockPosition()), TFCTags.Fluids.ANY_INFINITE_WATER))
        {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        if (!level.isClientSide)
        {
            player.displayClientMessage(Component.translatable("tfc.tooltip.pan.water"), true);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int countLeft)
    {
        if (countLeft % 16 == 0 && !level.isClientSide)
        {
            level.playSound(null, entity, TFCSounds.PANNING.get(), SoundSource.PLAYERS, 1f, 1f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (entity instanceof Player player && level instanceof ServerLevel serverLevel)
        {
            final Pannable pannable = readPannable(level.holderLookup(Registries.BLOCK), stack);
            if (pannable != null)
            {
                final var table = level.getServer().getLootData().getLootTable(pannable.getLootTable());
                final var builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.TOOL, stack);
                final List<ItemStack> items = table.getRandomItems(builder.create(LootContextParamSets.FISHING));
                items.forEach(item -> ItemHandlerHelper.giveItemToPlayer(player, item));
                player.awardStat(Stats.ITEM_USED.get(this));
                return new ItemStack(TFCItems.EMPTY_PAN.get()); // MC calls setItemInHand to place this in the hand
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> text, TooltipFlag flag)
    {
        if (level == null)
        {
            return;
        }
        final BlockState state = readState(level.holderLookup(Registries.BLOCK), stack);
        if (state != null)
        {
            text.add(Component.translatable("tfc.tooltip.pan.contents").append(state.getBlock().getName()));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions() {
            private final NonNullLazy<PanItemRenderer> renderer = NonNullLazy.of(PanItemRenderer::new);
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return renderer.get();
            }
        });
    }
}
