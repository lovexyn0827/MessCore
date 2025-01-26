package lovexyn0827.mess.options;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.context.CommandContext;
import lovexyn0827.mess.MessMod;
import lovexyn0827.mess.mixins.WorldSavePathMixin;
import lovexyn0827.mess.util.access.AccessingPath;
import lovexyn0827.mess.util.blame.BlamingMode;
import lovexyn0827.mess.util.blame.Confidence;
import lovexyn0827.mess.util.i18n.I18N;
import lovexyn0827.mess.util.i18n.Language;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * <p>There are three layers in the option storage, that is, hard-coded global default values, global option value, and the save-local values.</p>
 * All fields and methods here are declared static in order to speed up option reading.
 * @author lovexyn0827
 * Date: April 2, 2022
 */

// TODO flowerFieldRenderer & 

public class OptionManager {
	private static final Set<Class<?>> OPTION_CLASSES = Sets.newHashSet();
	public static final SortedMap<String, OptionWrapper> OPTIONS;
	private static OptionSet activeOptionSet;
	
	/**
	 * Actions taken right after an option is set to a given value.
	 */
	static final Map<String, CustomOptionApplicator> CUSTOM_APPLICATION_BEHAVIORS = Maps.newHashMap();
	static final Map<String, CustomOptionValidator> CUSTOM_OPTION_VALIDATORS = Maps.newHashMap();

	@Option(defaultValue = "true", 
			parserClass = BooleanParser.class, 
			label = Label.MESSMOD)
	public static boolean accessingPathDynamicAutoCompletion;
	
	@Option(defaultValue = "STANDARD", 
			parserClass = AccessingPath.InitializationStrategy.Parser.class, 
			label = Label.MESSMOD)
	public static AccessingPath.InitializationStrategy accessingPathInitStrategy;
	
	@Option(defaultValue = "POSSIBLE", 
			parserClass = Confidence.Parser.class, 
			label = Label.MESSMOD)
	public static Confidence blameThreshold;
	
	@Option(defaultValue = "DISABLED", 
			parserClass = BlamingMode.Parser.class, 
			label = Label.MESSMOD)
	public static BlamingMode blamingMode;
	
	@Option(defaultValue = "true", 
			parserClass = BooleanParser.class, 
			label = Label.MESSMOD)
	public static boolean commandExecutionRequirment;

	@Option(defaultValue = "true", 
			parserClass = BooleanParser.class, 
			label = { Label.RESEARCH, Label.MESSMOD })
	public static boolean directChunkAccessForMessMod;
	
	@Option(defaultValue = "false", 
			parserClass = BooleanParser.class, 
			globalOnly = true, 
			environment = EnvType.CLIENT, 
			label = { Label.MISC, Label.MESSMOD })
	public static boolean hideSurvivalSaves;
		
	@Option(defaultValue = "-FOLLOW_SYSTEM_SETTINGS-", 
			parserClass = Language.Parser.class, 
			label = Label.MESSMOD)
	public static String language;
	
	// TODO
	@Option(defaultValue = "false", 
			experimental = true, 
			parserClass = BooleanParser.class, 
			label = Label.MESSMOD)
	public static boolean strictAccessingPathParsing;
	
	@Option(defaultValue = "false", 
			experimental = true, 
			parserClass = BooleanParser.class, 
			label = Label.MISC)
	public static boolean superSuperSecretSetting;
		
	private static void setOptionSet(OptionSet set) {
		activeOptionSet = set;
		set.activiate();
		if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
			MessMod.LOGGER.info("Loaded {} MessMod config from {}", 
					OPTIONS.size(), set.getReadablePathStr());
			OPTIONS.values().stream()
					.map((o) -> {
						return o.name + ": " + set.getSerialized(o.name);
					})
					.forEach(MessMod.LOGGER::info);
		}
	}
	
	public static boolean isValidOptionName(String name) {
		return OPTIONS.containsKey(name);
	}
	
	public static void onReceivedOptions(PacketByteBuf in) throws IOException {
		setOptionSet(OptionSet.fromPacket(in));
	}
	
	public static void registerCustomApplicator(String name, CustomOptionApplicator behavior) {
		CUSTOM_APPLICATION_BEHAVIORS.put(name, behavior);
	}
	
	public static void registerCustomValidator(String name, CustomOptionValidator validator) {
		CUSTOM_OPTION_VALIDATORS.put(name, validator);
	}
	
	public static void registerCustomHandlers(String name, CustomOptionValidator validator, 
			CustomOptionApplicator behavior) {
		CUSTOM_APPLICATION_BEHAVIORS.put(name, behavior);
		CUSTOM_OPTION_VALIDATORS.put(name, validator);
	}
	
	/**
	 * @param ms The current single-player server, or null if exiting.
	 */
	public static void updateServer(@Nullable MinecraftServer ms) {
		if(ms != null) {
			Path p = ms.getSavePath(WorldSavePathMixin.create("mcwmem.prop"));
			setOptionSet(OptionSet.load(p.toFile()));
		} else {
			activeOptionSet.save();
			setOptionSet(OptionSet.GLOBAL);
		}
	}
	
	public static void loadFromRemoteServer(PacketByteBuf data) {
		setOptionSet(OptionSet.fromPacket(data));
	}
	
	public static void loadSingleFromRemoteServer(PacketByteBuf data) {
		if(activeOptionSet == OptionSet.GLOBAL) {
			MessMod.LOGGER.error("Trying to load options to global option set!");
			return;
		}
		
		String name = data.readString();
		String value = data.readString();
		try {
			activeOptionSet.set(name, value);
		} catch (InvalidOptionException e) {
			MessMod.LOGGER.error("Received incorrect option {}={}: {}", name, value, e.getLocalizedMessage());
		}
	}
	
	public static void reload() {
		activeOptionSet.reload();
	}
	
	public static void sendOptionsTo(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(activeOptionSet.toPacket());
	}
	
	public static OptionSet getActiveOptionSet() {
		return activeOptionSet;
	}
	
	public static OptionSet getGlobalOptionSet() {
		return OptionSet.GLOBAL;
	}
	
	static{
		registerCustomHandlers("language", (newVal, ct) -> {
			boolean forceLoad = ((String) newVal).endsWith(Language.FORCELOAD_SUFFIX);
			String id = ((String) newVal).replace(Language.FORCELOAD_SUFFIX, "");
			if(!I18N.canUseLanguage(id, forceLoad)) {
				throw new InvalidOptionException("Language " + id + " is unsupported or incomplete.");
			}
		}, (newVal, ct) -> {
			boolean forceLoad = ((String) newVal).endsWith(Language.FORCELOAD_SUFFIX);
			String id = ((String) newVal).replace(Language.FORCELOAD_SUFFIX, "");
			if(!I18N.setLanguage(id, forceLoad)) {
				throw new IllegalStateException("Option language is not validated!");
			}
		});
		OPTION_CLASSES.add(OptionManager.class);
		MessMod.getComponents().forEach((c) -> {
			Class<?> cl = c.getOptionClass();
			if (cl == null) {
				return;
			}
			
			if (!OPTION_CLASSES.add(c.getOptionClass())) {
				throw new IllegalStateException("Option class " + cl + " has already been registered.");
			}
		});
		OPTIONS = OPTION_CLASSES.stream()
					.map(Class::getFields)
					.flatMap(Stream::of)
					.filter((f) -> f.isAnnotationPresent(Option.class))
					.sorted((a, b) -> Comparator.<String>naturalOrder().compare(a.getName(), b.getName()))
					.collect(TreeMap::new, (map, f) -> map.put(f.getName(), new OptionWrapper(f)), Map::putAll);
		OPTIONS.values().forEach((o) -> {
			if(!I18N.EN_US.containsKey(String.format("opt.%s.desc", o.name))) {
				MessMod.LOGGER.warn("The description of option {} is missing!", o.name);
			}
		});
		setOptionSet(OptionSet.GLOBAL);
	}
	
	@FunctionalInterface interface CustomOptionApplicator {
		/**
		 * Called after the new value of an option has been set. 
		 * This method mainly perform custom application steps other than changing the value of an option.
		 * @param newValue The new value, previously validated.
		 */
		void onOptionUpdate(Object newValue, @Nullable CommandContext<ServerCommandSource> ct);
	}
	
	@FunctionalInterface interface CustomOptionValidator {
		/**
		 * Perform some non-generic validations.
		 * @param oldValue The previous value of the updated option, or null not exist.
		 */
		void validate(@Nullable Object newVal, @Nullable CommandContext<ServerCommandSource> ct)
				throws InvalidOptionException;
	}
}
