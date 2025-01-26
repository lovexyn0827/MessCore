package lovexyn0827.mess;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import lovexyn0827.mess.network.MessClientNetworkHandler;
import lovexyn0827.mess.network.MessServerNetworkHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public interface MessComponent {
	String modid();
	
	default String inGameId() {
		return this.modid().toUpperCase();
	}
	
	@Nullable
	default Class<?> getOptionClass() {
		return null;
	}
	
	default Path languageFiles(String lang) {
		return FabricLoader.getInstance().getModContainer(this.modid())
				.get().getRootPath().resolve("assets/lang/" + lang + ".json");
	}
	
	default void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
	}
	
	default void runCommandResetters() {
	}
	
	default void registerArgumentTypes() {
	}
	
	default void registerServerPackerHandlers(BiConsumer<Identifier, MessServerNetworkHandler.PacketHandler> reg) {
	}
	
	default void registerClientPackerHandlers(BiConsumer<Identifier, MessClientNetworkHandler.PacketHandler> reg) {
	}
	
	default void registerF3KeyBindings(Char2ObjectMap<Runnable> registry) {
	}
	
	default boolean handleKeyBindings(MinecraftClient mc, int key, boolean ctrl, boolean alt) {
		return false;
	}
	
	default void onClientTicked() {	
	}
	
	default void onServerTicked(MinecraftServer server) {
	}
	
	default void onGuiRendering() {
	}
	
	default void onServerStarted(MinecraftServer server) {
	}
	
	default void onServerShutdown(MinecraftServer server) {
	}
	
	default void onPlayerRespawned() {
	}
	
	default void onServerPlayerSpawned(ServerPlayerEntity player) {
	}
	
	default void onDisconnected() {
	}

	default void onGameJoined() {
	}
}
