package com.quantumgarbage.gtmogs.integration.kjs.events;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import com.quantumgarbage.gtmogs.api.worldgen.BiomeWeightModifier;
import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import com.quantumgarbage.gtmogs.api.worldgen.generator.veins.NoopVeinGenerator;
import com.quantumgarbage.gtmogs.data.worldgen.GTOreVeins;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.Context;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class GTOreVeinKubeEvent implements KubeEvent {

    private final WritableRegistry<OreVeinDefinition> registry;
    private final HolderLookup.Provider holderProvider;

    public GTOreVeinKubeEvent(WritableRegistry<OreVeinDefinition> registry,
                              HolderLookup.Provider holderProvider) {
        this.registry = registry;
        this.holderProvider = holderProvider;
    }

    public void add(Context cx, ResourceLocation id, Consumer<OreVeinDefinition> consumer) {
        var biomes = holderProvider.lookupOrThrow(Registries.BIOME);

        var vein = GTOreVeins.blankOreDefinition(biomes);
        consumer.accept(vein);

        registry.register(GTOreVeins.create(id), vein, RegistrationInfo.BUILT_IN);
    }

    public void modify(Context cx, ResourceLocation id, Consumer<OreVeinDefinition> consumer) {
        var biomes = holderProvider.lookupOrThrow(Registries.BIOME);

        var vein = registry.get(id);
        if (vein == null) throw new IllegalArgumentException("Ore vein doesn't exist: " + id);

        vein.biomeLookup(biomes);
        consumer.accept(vein);
    }

    public void modifyAll(Context cx, BiConsumer<ResourceLocation, OreVeinDefinition> consumer) {
        var biomes = holderProvider.lookupOrThrow(Registries.BIOME);

        Set<ResourceLocation> keys = registry.keySet();
        keys.forEach(id -> {
            var vein = registry.get(id);
            if (vein == null) throw new IllegalArgumentException("Ore vein doesn't exist: " + id);
            vein.biomeLookup(biomes);
            consumer.accept(id, vein);
        });
    }

    public void remove(Context cx, ResourceLocation id) {
        remove(cx, registry, id);
    }

    public void removeAll(Context cx) {
        Set<ResourceLocation> keys = Set.copyOf(registry.keySet());
        keys.forEach(key -> remove(cx, registry, key));
    }

    public void removeAll(Context cx, BiPredicate<ResourceLocation, OreVeinDefinition> predicate) {
        Set<ResourceLocation> keys = Set.copyOf(registry.keySet());
        keys.stream()
                .filter(key -> predicate.test(key, registry.get(key)))
                .forEach(key -> remove(cx, registry, key));
    }

    private void remove(Context cx, Registry<OreVeinDefinition> registry, ResourceLocation id) {
        if (!registry.containsKey(id)) {
            ConsoleJS.SERVER.error("", new KubeRuntimeException("Trying to remove nonexistent bedrock ore vein " + id)
                    .source(SourceLine.of(cx)));
            return;
        }
        // blank out the vein info because we can't remove from the registry
        var holder = registry.getHolderOrThrow(GTOreVeins.create(id));
        holder.value().veinGenerator(NoopVeinGenerator.INSTANCE);
        holder.value().biomeWeightModifier(BiomeWeightModifier.EMPTY);
        holder.value().weight(0);
    }
}
