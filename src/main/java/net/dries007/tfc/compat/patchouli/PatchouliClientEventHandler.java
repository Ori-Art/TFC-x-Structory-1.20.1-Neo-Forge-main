/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.compat.patchouli;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.network.OpenFieldGuidePacket;
import net.dries007.tfc.network.PacketHandler;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.base.PatchouliConfig;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.ItemModBook;
import vazkii.patchouli.common.util.ItemStackUtil;

/**
 * This is modified from {@link vazkii.patchouli.client.handler.TooltipHandler}
 * We want to render the tooltip additionally
 * - When patchouli otherwise wouldn't be rendering a tooltip, and
 * - When the book in question is ours
 */
public final class PatchouliClientEventHandler
{
    private static float lexiconLookupTime = 0;

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(PatchouliClientEventHandler::renderBookTooltipWithoutBook);
    }

    public static void renderBookTooltipWithoutBook(RenderTooltipEvent.Pre event)
    {
        if (!TFCConfig.CLIENT.showGuideBookLinksAlways.get()) return;

        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics graphics = event.getGraphics();
        final ItemStack stack = event.getItemStack();
        final int tooltipX = event.getX();
        final int tooltipY = event.getY() - 4;

        if (wouldPatchouliRenderATooltipHere(minecraft, stack))
        {
            return;
        }

        // We only are concerned with our own book
        final Book book = BookRegistry.INSTANCE.books.get(PatchouliIntegration.BOOK_ID);
        final Pair<BookEntry, Integer> entry = book.getContents().getEntryForStack(stack);

        if (entry != null && !entry.getFirst().isLocked())
        {
            final ItemStack bookStack = ItemModBook.forBook(book);

            int x = tooltipX - 34;
            RenderSystem.disableDepthTest();

            graphics.fill(x - 4, tooltipY - 4, x + 20, tooltipY + 26, 0x44000000);
            graphics.fill(x - 6, tooltipY - 6, x + 22, tooltipY + 28, 0x44000000);

            if (PatchouliConfig.get().useShiftForQuickLookup() ? Screen.hasShiftDown() : Screen.hasControlDown())
            {
                lexiconLookupTime += ClientTicker.delta;

                int cx = x + 8;
                int cy = tooltipY + 8;
                float r = 12;
                float time = 20F;
                float angles = lexiconLookupTime / time * 360F;

                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                BufferBuilder buf = Tesselator.getInstance().getBuilder();
                buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

                float a = 0.5F + 0.2F * ((float) Math.cos(ClientTicker.total / 10) * 0.5F + 0.5F);
                buf.vertex(cx, cy, 0).color(0F, 0.5F, 0F, a).endVertex();

                for (float i = angles; i > 0; i--)
                {
                    double rad = (i - 90) / 180F * Math.PI;
                    buf.vertex(cx + Math.cos(rad) * r, cy + Math.sin(rad) * r, 0).color(0F, 1F, 0F, 1F).endVertex();
                }

                buf.vertex(cx, cy, 0).color(0F, 1F, 0F, 0F).endVertex();
                Tesselator.getInstance().end();

                RenderSystem.disableBlend();

                if (lexiconLookupTime >= time)
                {
                    // Change: don't move the selected slot, because we're not opening from a stack
                    // minecraft.player.getInventory().selected = lexSlot;
                    final int spread = entry.getSecond();

                    // Change: don't open the book client side only
                    // See Issue/2152
                    PacketHandler.send(PacketDistributor.SERVER.noArg(), new OpenFieldGuidePacket(entry.getFirst().getId(), spread * 2));
                }
            }
            else
            {
                lexiconLookupTime = 0F;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.renderItem(bookStack, x, tooltipY);
            graphics.renderItemDecorations(minecraft.font, bookStack, x, tooltipY);
            graphics.pose().popPose();

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            graphics.drawString(minecraft.font, "?", x + 10, tooltipY + 8, 0xFFFFFFFF, true);

            graphics.pose().scale(0.5F, 0.5F, 1F);
            boolean mac = Minecraft.ON_OSX;
            Component key = Component.literal(PatchouliConfig.get().useShiftForQuickLookup() ? "Shift" : mac ? "Cmd" : "Ctrl")
                .withStyle(ChatFormatting.BOLD);
            graphics.drawString(minecraft.font, key, (x + 10) * 2 - 16, (tooltipY + 8) * 2 + 20, 0xFFFFFFFF, true);
            graphics.pose().popPose();

            RenderSystem.enableDepthTest();
        }
        else
        {
            lexiconLookupTime = 0F;
        }
    }

    /**
     * @return {@code true} if {@link vazkii.patchouli.client.handler.TooltipHandler} would render a tooltip, so we don't try and render one on top of it
     */
    private static boolean wouldPatchouliRenderATooltipHere(Minecraft minecraft, ItemStack stack)
    {
        if (minecraft.player != null && !(minecraft.screen instanceof GuiBook))
        {
            for (int i = 0; i < Inventory.getSelectionSize(); i++)
            {
                final ItemStack stackAt = minecraft.player.getInventory().getItem(i);
                if (!stackAt.isEmpty())
                {
                    final Book book = ItemStackUtil.getBookFromStack(stackAt);
                    if (book != null)
                    {
                        final Pair<BookEntry, Integer> entry = book.getContents().getEntryForStack(stack);
                        if (entry != null && !entry.getFirst().isLocked())
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
