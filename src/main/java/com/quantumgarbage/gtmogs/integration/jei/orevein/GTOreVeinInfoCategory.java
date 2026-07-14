package com.quantumgarbage.gtmogs.integration.jei.orevein;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.quantumgarbage.gtmogs.GTMOGS;
import com.quantumgarbage.gtmogs.api.registry.GTRegistries;
import com.quantumgarbage.gtmogs.api.worldgen.DimensionMarker;
import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import com.quantumgarbage.gtmogs.integration.xei.widgets.GTOreVeinWidget;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GTOreVeinInfoCategory implements IRecipeCategory<Holder<OreVeinDefinition>> {
    static RecipeType<Holder<OreVeinDefinition>> RECIPE_TYPE = new RecipeType<Holder<OreVeinDefinition>>(
            GTMOGS.id("ore_vein_diagram"),
            (Class<Holder<OreVeinDefinition>>) (Class<?>) Holder.class);

    private static final int X = 5;
    private static final int Y_START = 5;
    private static final int SLOT_SIZE = 18;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable background;
    private final IDrawable icon;

    public GTOreVeinInfoCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(GTOreVeinWidget.WIDTH, 140);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        var ores = Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(GTRegistries.ORE_VEIN_REGISTRY);
        registry.addRecipes(RECIPE_TYPE, ores.holders()
                .filter(ore -> ore.value().canGenerate())
                .<Holder<OreVeinDefinition>>map(Function.identity())
                .toList());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Holder<OreVeinDefinition> definition, IFocusGroup focuses) {
        var widget = new GTOreVeinWidget(definition);

        // Register ore items as OUTPUT slots (clickable, searchable)
        var ores = widget.getContainedOres();
        for (int i = 0; i < ores.size(); i++) {
            int slotX = X + i * SLOT_SIZE;
            builder.addSlot(RecipeIngredientRole.OUTPUT, slotX, Y_START + 16)
                    .addItemStack(ores.get(i));
        }

        // Register dimension markers as CATALYST slots (clickable, searchable)
        int dimY = Y_START + 16 + SLOT_SIZE + 4 + LINE_HEIGHT * 2 + LINE_HEIGHT * 2 + LINE_HEIGHT;
        var dimMarkers = widget.getDimensionMarkers();
        for (int i = 0; i < dimMarkers.length; i++) {
            int slotX = X + (i % 6) * SLOT_SIZE;
            int row = i / 6;
            builder.addSlot(RecipeIngredientRole.CATALYST, slotX, dimY + row * SLOT_SIZE)
                    .addItemStack(dimMarkers[i].getIcon());
        }
    }

    @Override
    public void draw(Holder<OreVeinDefinition> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        var widget = new GTOreVeinWidget(recipe);
        Font font = Minecraft.getInstance().font;
        int y = Y_START;

        // Title
        graphics.drawString(font, widget.getTranslationKey(), X, y, TEXT_COLOR, false);
        y += 14;

        // ore slots are rendered by JEI between y=16 and y=16+18
        y += SLOT_SIZE + 4;

        // Spawn range label
        graphics.drawString(font,
                Component.translatable("gtmogs.jei.ore_vein_diagram.spawn_range").getString(),
                X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;
        graphics.drawString(font, widget.getRange(), X + 5, y, TEXT_COLOR, false);
        y += LINE_HEIGHT + 2;

        // Weight
        graphics.drawString(font,
                Component.translatable("gtmogs.jei.ore_vein_diagram.weight", widget.getWeight()).getString(),
                X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT + 2;

        // Dimensions label
        graphics.drawString(font,
                Component.translatable("gtmogs.jei.ore_vein_diagram.dimensions").getString(),
                X, y, TEXT_COLOR, false);
        // dim marker slots are rendered by JEI below this line
    }

    @NotNull
    @Override
    public RecipeType<Holder<OreVeinDefinition>> getRecipeType() {
        return RECIPE_TYPE;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("gtmogs.jei.ore_vein_diagram");
    }

    @Override
    public int getWidth() {
        return GTOreVeinWidget.WIDTH;
    }

    @Override
    public int getHeight() {
        return 140;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }
}
