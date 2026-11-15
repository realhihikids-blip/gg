package com.pvpclient.mixin;

import com.pvpclient.util.AimSmoother;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.class)
public class PlayerMoveC2SPacketMixin {
    
    @Shadow protected float yaw;
    @Shadow protected float pitch;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPacketCreation(CallbackInfo ci) {
        // Если в чите включена тихая наводка — подменяем углы в пакете перед отправкой на сервер
        if (AimSmoother.isSilentRotating) {
            this.yaw = AimSmoother.silentYaw;
            this.pitch = AimSmoother.silentPitch;
        }
    }
}