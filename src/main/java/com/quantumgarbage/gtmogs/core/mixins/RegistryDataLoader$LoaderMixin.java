package com.quantumgarbage.gtmogs.core.mixins;

import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;

import com.quantumgarbage.gtmogs.core.MixinHelpers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.resources.RegistryDataLoader$Loader")
public class RegistryDataLoader$LoaderMixin<T> {

    @Shadow
    private @Final WritableRegistry<T> registry;

    @Inject(method = "loadFromResources", at = @At("TAIL"))
    private void gtmogs$postKJSVeinEvents(ResourceManager resourceManager,
                                          RegistryOps.RegistryInfoLookup registryInfoLookup, CallbackInfo ci) {
        MixinHelpers.postKJSVeinEvents(this.registry, registryInfoLookup);
    }
}
