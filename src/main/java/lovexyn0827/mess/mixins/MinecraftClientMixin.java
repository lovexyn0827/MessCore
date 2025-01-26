package lovexyn0827.mess.mixins;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.mess.MessMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.hit.HitResult;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow @Final ClientPlayerEntity player;
	@Shadow @Final IntegratedServer server;
	@Shadow @Final HitResult crosshairTarget;
	@Shadow private int itemUseCooldown;
	
	@Shadow abstract Window getWindow();
	
	@Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blit"))
	private void onRender(boolean tick,CallbackInfo ci) {
		MessMod.INSTANCE.onRender(this.player, this.server);
	}
	
	@Inject(method = "tick", at = @At(value = "HEAD"))
	private void onTickStart(CallbackInfo ci) {
		MessMod.INSTANCE.onClientTickStart();
	}

	@Inject(
			method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", 
			at = @At(value = "HEAD"))
	private void onDisconnected(Screen screen, CallbackInfo ci) {
		MessMod.INSTANCE.onDisconnected();
	}
}