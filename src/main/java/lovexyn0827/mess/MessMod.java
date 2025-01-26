package lovexyn0827.mess;

import java.util.Collection;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import lovexyn0827.mess.command.CommandUtil;
import lovexyn0827.mess.mixins.WorldSavePathMixin;
import lovexyn0827.mess.network.MessClientNetworkHandler;
import lovexyn0827.mess.network.MessServerNetworkHandler;
import lovexyn0827.mess.options.OptionManager;
import lovexyn0827.mess.rendering.ShapeCache;
import lovexyn0827.mess.rendering.ShapeRenderer;
import lovexyn0827.mess.rendering.ShapeSender;
import lovexyn0827.mess.util.LockableList;
import lovexyn0827.mess.util.access.CustomNode;
import lovexyn0827.mess.util.deobfuscating.Mapping;
import lovexyn0827.mess.util.deobfuscating.MappingProvider;
import lovexyn0827.mess.util.phase.ClientTickingPhase;
import lovexyn0827.mess.util.phase.ServerTickingPhase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class MessMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final MessMod INSTANCE = new MessMod();
	private static final LockableList<MessComponent> COMPONENTS = LockableList.create();
	private Mapping mapping;
	@Nullable
	private MinecraftServer server;
	private String scriptDir;
	@Environment(EnvType.CLIENT)
	public ShapeRenderer shapeRenderer;	// Reading from the field directly may bring higher performance.
	@Environment(EnvType.CLIENT)
	public ShapeCache shapeCache;
	public ShapeSender shapeSender;
	@Environment(EnvType.CLIENT)
	private MessClientNetworkHandler clientNetworkHandler;
	private MessServerNetworkHandler serverNetworkHandler;
	private long gameTime;

	private MessMod() {
	}

	public void reloadMapping() {
		this.mapping = new MappingProvider().tryLoadMapping();
	}

	@Override
	public void onInitialize() {
		this.reloadMapping();
	}
	
	public Mapping getMapping() {
		return this.mapping;
	}
	
	public static boolean registerComponent(MessComponent comp) {
		if (!FabricLoader.getInstance().isModLoaded(comp.modid())) {
			LOGGER.error("MessComponent {} has incorrect modid", comp.modid());
			return false;
		}
		
		if (COMPONENTS.add(comp)) {
			return true;
		} else {
			LOGGER.warn("MessComponent {} has already been added.", comp.modid());
			return false;
		}
	}
	
	public static Collection<MessComponent> getComponents() {
		if (COMPONENTS.isLocked()) {
			return COMPONENTS;
		} else {
			throw new IllegalStateException("Trying to get component list before initialization finishes!");
		}
	}
	
	public static void lockComponentList() {
		COMPONENTS.lock();
	}
	
	//************ SERVER SIDE *****************
	
	public void onServerTicked(MinecraftServer server) {
		this.shapeSender.updateClientTime(server.getOverworld().getTime());
		getComponents().forEach((c) -> c.onServerTicked(server));
	}
	

	public void onServerStarted(MinecraftServer server) {
		this.server = server;
		CommandUtil.updateServer(server);
		OptionManager.updateServer(server);
		this.serverNetworkHandler = new MessServerNetworkHandler(server);
		CustomNode.reload(server);
		this.shapeSender = ShapeSender.create(server);
		getComponents().forEach((c) -> c.onServerStarted(server));
	}

	public void onServerShutdown(MinecraftServer server) {
		this.server = null;
		this.serverNetworkHandler = null;
		ServerTickingPhase.initialize();
		CommandUtil.updateServer(null);
		OptionManager.updateServer(null);
		getComponents().forEach((c) -> c.onServerShutdown(server));
	}

	public void onServerPlayerSpawned(ServerPlayerEntity player) {
		if(isDedicatedServerEnv()) {
			OptionManager.sendOptionsTo(player);
		}
		
		CommandUtil.tryUpdatePlayer(player);
		this.scriptDir = server.getSavePath(WorldSavePathMixin.create("scripts")).toAbsolutePath().toString();
		getComponents().forEach((c) -> c.onServerPlayerSpawned(player));
	}

	//************ CLIENT SIDE *****************
	
	@Environment(EnvType.CLIENT)
	public void onRender(ClientPlayerEntity player, IntegratedServer server) {
		getComponents().forEach((c) -> c.onGuiRendering());
	}
	
	@Environment(EnvType.CLIENT)
	public void onGameJoined(GameJoinS2CPacket packet) {
		MinecraftClient mc = MinecraftClient.getInstance();
		this.clientNetworkHandler = new MessClientNetworkHandler(mc);
		this.clientNetworkHandler.sendVersion();
		ShapeRenderer sr = new ShapeRenderer(mc);
        this.shapeRenderer = sr;
        this.shapeCache = sr.getShapeCache();
        getComponents().forEach((c) -> c.onGameJoined());
	}

	@Environment(EnvType.CLIENT)
	public void onClientTickStart() {
		ClientTickingPhase.CLIENT_TICK_START.begin(null);
	}

	@Environment(EnvType.CLIENT)
	public void onClientTicked() {
		ClientTickingPhase.CLIENT_TICK_END.begin(null);
		getComponents().forEach((c) -> c.onClientTicked());
	}

	//Client
	@Environment(EnvType.CLIENT)
	public void onDisconnected() {
		getComponents().forEach((c) -> c.onDisconnected());
	}
	
	//Client
	@Environment(EnvType.CLIENT)
	public void onPlayerRespawned(PlayerRespawnS2CPacket packet) {
		getComponents().forEach((c) -> c.onPlayerRespawned());
	}

	public void sendMessageToEveryone(Object... message) {
		if(this.server == null) {
			throw new IllegalStateException("Called without a server started!");
		}
		
		StringBuilder sb = new StringBuilder();
		for(Object ob : message) {
			sb.append(ob);
		}
		
		this.server.getPlayerManager().broadcastChatMessage(new LiteralText(sb.toString()), 
				MessageType.SYSTEM, 
				new UUID(0x31f38bL,0x31f0b8L));
	}

	public String getScriptDir() {
		return this.scriptDir;
	}
	
	/**
	 * @return Whether or not the running environment is a dedicated server or a client that has joined to a dedicated server
	 */
	public static boolean isDedicatedEnv() {
		if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			return true;
		} else {
			MinecraftClient mc = MinecraftClient.getInstance();
			return mc.getServer() == null && (mc.getCurrentServerEntry() == null ? 
					false : !mc.getCurrentServerEntry().isLocal());
		}
	}

	@Environment(EnvType.CLIENT)
	public void onDisconnect(DisconnectS2CPacket packet) {
		this.shapeCache.reset();
		this.clientNetworkHandler = null;
	}

	public MessServerNetworkHandler getServerNetworkHandler() {
		return this.serverNetworkHandler;
	}

	@Environment(EnvType.CLIENT)
	public MessClientNetworkHandler getClientNetworkHandler() {
		return this.clientNetworkHandler;
	}
	
	public static boolean isDedicatedServerEnv() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	public boolean isOnThread(NetworkSide side) {
		if(side == NetworkSide.CLIENTBOUND) {
			return MinecraftClient.getInstance().isOnThread();
		} else {
			return this.server != null ? this.server.isOnThread() : false;
		}
	}

	public long getGameTime() {
		return this.gameTime;
	}

	public void updateTime(long time) {
		this.gameTime = time;
	}
}
