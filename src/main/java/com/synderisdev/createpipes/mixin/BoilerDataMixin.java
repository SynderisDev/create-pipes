package com.synderisdev.createpipes.mixin;

import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.synderisdev.createpipes.content.intake.SteamIntakeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoilerData.class)
public abstract class BoilerDataMixin {
    @Shadow
    public int attachedWhistles;

    @Shadow
    public boolean needsHeatLevelUpdate;

    @Inject(method = "evaluate", at = @At("RETURN"), cancellable = true)
    private void createPipes$countSteamIntakes(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        int intakes = SteamIntakeBlockEntity.countAttachedIntakes(controller);
        if (intakes <= 0) {
            return;
        }
        attachedWhistles += intakes;
        needsHeatLevelUpdate = true;
        cir.setReturnValue(true);
    }
}
