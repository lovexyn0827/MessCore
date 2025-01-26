package lovexyn0827.mess.mixins;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import lovexyn0827.mess.MessMod;
import lovexyn0827.mess.command.AccessingPathCommand;
import lovexyn0827.mess.command.MessCfgCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

@Mixin(CommandManager.class)
public abstract class CommandMixin {
	@Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void registerCommand(CommandManager.RegistrationEnvironment regEnv, CallbackInfo info) {
        MessCfgCommand.register(this.dispatcher);
        AccessingPathCommand.register(this.dispatcher);
		MessMod.getComponents().forEach((c) -> c.registerCommands(this.dispatcher));
    }
    
    @Redirect(method = "execute", 
    		at = @At(
    				value = "INVOKE",
    				target = "org/apache/logging/log4j/Logger.isDebugEnabled()V", 
    				remap = false
    		),
    		require = 0
    )
    private boolean alwaysOutputStackTrace(Logger l) {
    	return true;
    }
}
