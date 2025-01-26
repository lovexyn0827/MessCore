package lovexyn0827.mess.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.mess.MessMod;
import net.minecraft.server.Main;

@Mixin(Main.class)
public class ServerMainMixin {
	@Inject(method = "main", at = @At("HEAD"))
	private static void onClientStart(String[] args, CallbackInfo ci) {
		MessMod.lockComponentList();
	}
}
