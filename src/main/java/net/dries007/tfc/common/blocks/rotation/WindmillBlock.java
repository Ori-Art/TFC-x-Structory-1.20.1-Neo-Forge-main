/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.rotation;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.common.blockentities.rotation.WindmillBlockEntity;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.devices.DeviceBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.advancements.TFCAdvancements;

public class WindmillBlock extends DeviceBlock implements EntityBlockExtension, ConnectedAxleBlock
{
    public static final IntegerProperty COUNT = TFCBlockStateProperties.COUNT_1_5;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private final Supplier<? extends AxleBlock> axle;

    public WindmillBlock(ExtendedProperties properties, Supplier<? extends AxleBlock> axle)
    {
        super(properties, InventoryRemoveBehavior.DROP);

        this.axle = axle;

        registerDefaultState(getStateDefinition().any().setValue(COUNT, 1));
    }

    @Override
    public AxleBlock getAxle()
    {
        return axle.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof RotatingBlockEntity entity)
        {
            entity.destroyIfInvalid(level, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result)
    {
        final ItemStack stack = player.getItemInHand(hand);
        final int count = state.getValue(COUNT);
        if (level.getBlockEntity(pos) instanceof WindmillBlockEntity windmill)
        {
            final IItemHandler inv = Helpers.getCapability(windmill, Capabilities.ITEM);
            if (inv != null)
            {
                if (count < WindmillBlockEntity.SLOTS && !stack.isEmpty())
                {
                    final ItemStack leftover = Helpers.insertAllSlots(inv, player.isCreative() ? stack.copyWithCount(1) : stack.split(1));
                    if (!leftover.isEmpty())
                    {
                        ItemHandlerHelper.giveItemToPlayer(player, leftover);
                        return InteractionResult.PASS;
                    }
                    final int newCount = windmill.updateState();
                    if (newCount == WindmillBlockEntity.SLOTS && player instanceof ServerPlayer server)
                    {
                        TFCAdvancements.MAX_WINDMILL.trigger(server);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (count == WindmillBlockEntity.SLOTS || stack.isEmpty())
                {
                    ItemHandlerHelper.giveItemToPlayer(player, inv.extractItem(count - 1, 1, false));
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(AXIS, COUNT));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return state.getValue(AXIS) == Direction.Axis.X ? AxleBlock.SHAPE_X : AxleBlock.SHAPE_Z;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        return new ItemStack(TFCItems.WINDMILL_BLADE.get());
    }
}
