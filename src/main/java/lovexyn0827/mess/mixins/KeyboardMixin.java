package lovexyn0827.mess.mixins;

import java.util.Iterator;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import lovexyn0827.mess.MessComponent;
import lovexyn0827.mess.MessMod;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
	@Shadow @Final MinecraftClient client;
	
	private final Char2ObjectMap<Runnable> f3KeyBinds = new Char2ObjectOpenHashMap<>();
	
	@Inject(method = "processF3", at = @At(value = "HEAD"), cancellable = true)
	private void onF3Pressed(int key, CallbackInfoReturnable<Boolean> cir) {
		Runnable handler = this.f3KeyBinds.get((char) key);
		if (handler != null) {
			handler.run();
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
	
	@Inject(method = "<init>", at = @At("RETURN"))
	private void registerF3KeyBinds(MinecraftClient client, CallbackInfo ci) {
		MessMod.getComponents().forEach((c) -> c.registerF3KeyBindings(this.f3KeyBinds));
	}
	

	@Inject(method = "onKey", at = @At("RETURN"))
	private void handleKey(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
		if (i != GLFW.GLFW_PRESS) {
			return;
		}
		
		MinecraftClient mc = MinecraftClient.getInstance();
		Iterator<MessComponent> components = MessMod.getComponents().iterator();
		while (components.hasNext()) {
			if (components.next().handleKeyBindings(mc, key, Screen.hasControlDown(), Screen.hasAltDown())) {
				break;
			}
		}
	}
}
