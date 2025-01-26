package lovexyn0827.mess.mixins;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.mess.MessMod;
import lovexyn0827.mess.fakes.ServerWorldInterface;
import lovexyn0827.mess.util.NoChunkLoadingWorld;
import lovexyn0827.mess.util.phase.ServerTickingPhase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements BlockView, ServerWorldInterface {
	private @Final NoChunkLoadingWorld noChunkLoadingWorld;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	public void onCreated(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, 
			ServerWorldProperties properties, RegistryKey<World> registryKey, DimensionType dimensionType, 
			WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, 
			boolean debugWorld, long l, List<Spawner> list, boolean bl, CallbackInfo ci) {
		this.noChunkLoadingWorld = new NoChunkLoadingWorld((ServerWorld) (Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At("HEAD")
			)
	private void startTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.WEATHER_CYCLE.begin((ServerWorld)(Object) this);
		// Actually here is also the ending of WTU
		if(((ServerWorld)(Object) this).getRegistryKey() == World.OVERWORLD) {
			MessMod.INSTANCE.updateTime(((ServerWorld)(Object) this).getTime());
		}
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE_STRING", 
					target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", 
					args = "ldc=chunkSource")
			)
	private void startChunkTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.CHUNK.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE_STRING", 
					target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", 
					args = "ldc=tickPending")
			)
	private void startScheduledTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.SCHEDULED_TICK.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE_STRING", 
					target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", 
					args = "ldc=raid")
			)
	private void startVillageTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.VILLAGE.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE_STRING", 
					target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", 
					args = "ldc=blockEvents")
			)
	private void startBlockEventTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.BLOCK_EVENT.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE_STRING", 
					target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", 
					args = "ldc=entities")
			)
	private void startEntityTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.ENTITY.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tickBlockEntities()V")
			)
	private void startBlockEntityTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.TILE_ENTITY.begin((ServerWorld)(Object) this);
	}
	
	@Inject(method = "tick", 
			at = @At("RETURN")
			)
	private void endTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerTickingPhase.DIM_REST.begin((ServerWorld)(Object) this);
	}
	
	public NoChunkLoadingWorld toNoChunkLoadingWorld() {
		return this.noChunkLoadingWorld ;
	}
}
