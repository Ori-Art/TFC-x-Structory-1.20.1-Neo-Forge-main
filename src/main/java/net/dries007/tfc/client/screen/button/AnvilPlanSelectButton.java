/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.screen.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.screen.AnvilPlanScreen;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.ScreenButtonPacket;

public class AnvilPlanSelectButton extends Button
{
    private final ItemStack result;
    private final int page; // The page this button is on
    private final Component component;
    private int currentPage; // The page selected by the root gui

    public AnvilPlanSelectButton(int x, int y, int page, final AnvilRecipe recipe, Component tooltip)
    {
        super(x, y, 18, 18, tooltip, button -> {
            if (button.active)
            {
                final CompoundTag tag = new CompoundTag();
                tag.putString("recipe", recipe.getId().toString());
                PacketHandler.send(PacketDistributor.SERVER.noArg(), new ScreenButtonPacket(0, tag));
            }
        }, RenderHelpers.NARRATION);
        this.component = tooltip;
        setTooltip(Tooltip.create(tooltip));

        this.result = recipe.getResultItem(ClientHelpers.getLevelOrThrow().registryAccess());
        this.page = page;
        this.currentPage = 0;
    }

    public void setCurrentPage(int currentPage)
    {
        this.currentPage = currentPage;
        this.visible = this.active = this.currentPage == this.page;
    }

    public Component getComponent()
    {
        return component;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (this.visible)
        {
            int x = getX();
            int y = getY();
            graphics.blit(AnvilPlanScreen.BACKGROUND, x, y, 176, 0, width, height, 256, 256);
            graphics.renderItem(result, x + 1, y + 1);
            graphics.renderItemDecorations(Minecraft.getInstance().font, result, x + 1, y + 1);
        }
    }
}
