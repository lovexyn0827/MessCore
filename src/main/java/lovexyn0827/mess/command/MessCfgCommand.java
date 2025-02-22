package lovexyn0827.mess.command;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lovexyn0827.mess.MessMod;
import lovexyn0827.mess.MessModMixinPlugin;
import lovexyn0827.mess.options.InvalidOptionException;
import lovexyn0827.mess.options.Label;
import lovexyn0827.mess.options.OptionManager;
import lovexyn0827.mess.options.OptionWrapper;
import lovexyn0827.mess.util.FormattedText;
import lovexyn0827.mess.util.i18n.I18N;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class MessCfgCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		Command<ServerCommandSource> listAllCmd = (ct) -> {
			ModMetadata metadata = FabricLoader.getInstance().getModContainer("messcore").get().getMetadata();
			ServerCommandSource s = ct.getSource();
			s.sendFeedback(new LiteralText(metadata.getName() + " " + metadata.getVersion()).formatted(Formatting.BOLD), false);
			CommandUtil.feedbackWithArgs(ct, "cmd.messcfg.compcnt", MessMod.getComponents().size());
			s.sendFeedback(new FormattedText("cmd.messcfg.list", "l").asMutableText(), false);
			OptionManager.OPTIONS.forEach((name, opt) -> {
				dumpOption(s, name, opt);
			});
			return 1;
		};
		LiteralArgumentBuilder<ServerCommandSource> command = literal("messcfg").requires(CommandUtil.COMMAND_REQUMENT)
				.executes(listAllCmd)
				.then(literal("reloadConfig")
						.executes((ct) -> {
							OptionManager.reload();
							CommandUtil.feedback(ct, "cmd.messcfg.reload");
							return Command.SINGLE_SUCCESS;
						}))
				.then(literal("reloadMapping")
						.executes((ct) -> {
							MessMod.INSTANCE.reloadMapping();
							CommandUtil.feedback(ct, "cmd.messcfg.reloadmapping");
							return Command.SINGLE_SUCCESS;
						}))
				.then(literal("listComponents")
						.executes((ct) -> {
							CommandUtil.feedbackWithArgs(ct, "cmd.messcfg.compcnt", MessMod.getComponents().size());
							MessMod.getComponents().forEach((c) -> {
								ModMetadata meta = FabricLoader.getInstance()
										.getModContainer(c.modid())
										.get()
										.getMetadata();
								LiteralText line = new LiteralText(" - " + meta.getName());
								line.styled((s) -> {
									return s.withHoverEvent(new HoverEvent(
											HoverEvent.Action.SHOW_TEXT, new LiteralText(meta.getDescription())));
								});
								ct.getSource().sendFeedback(line, false);
							});
							return Command.SINGLE_SUCCESS;
						}))
				.then(literal("list")
						.executes(listAllCmd)
						.then(argument("label", StringArgumentType.word())
								.suggests(CommandUtil.immutableSuggestionsOfEnum(Label.class))
								.executes((ct) -> {
									Label label;
									String lName = StringArgumentType.getString(ct, "label");
									try {
										label = Label.valueOf(lName);
									} catch (IllegalArgumentException e) {
										CommandUtil.errorWithArgs(ct, "cmd.general.nodef", lName);
										return 0;
									}
									
									CommandUtil.feedbackWithArgs(ct, "cmd.messcfg.withtag", lName);
									ServerCommandSource s = ct.getSource();
									OptionManager.OPTIONS.forEach((name, opt) -> {
										for(Label l0 : opt.labels()) {
											if(l0 == label) {
												dumpOption(s, name, opt);
												break;
											}
										}
									});
									return Command.SINGLE_SUCCESS;
								})));
		OptionManager.OPTIONS.forEach((name, opt) -> {
			SuggestionProvider<ServerCommandSource> sp = opt.getSuggestions();
			command.then(literal(name).requires(CommandUtil.COMMAND_REQUMENT)
					.executes((ct) -> {
						MutableText text = new FormattedText(name, "a", false).asMutableText();
						if(opt.isExperimental()) {
							text.append(new FormattedText("cmd.messcfg.exp", "rcl").asMutableText());
						}
						
						text.append(new LiteralText("\n" + opt.getDescription() + "\n")
								.formatted(Formatting.GRAY));
						String value = OptionManager.getActiveOptionSet().getSerialized(name);
						text.append(new FormattedText("cmd.messcfg.current", "f", true, value).asMutableText());
						if(!opt.getDefaultValue().equals(value)) {
							text.append(new FormattedText("cmd.messcfg.modified", "cl").asMutableText());
						}
						
						String globalValue = OptionManager.getGlobalOptionSet().getSerialized(name);
						text.append(new FormattedText("cmd.messcfg.global", "f", true, globalValue).asMutableText());
						if(!opt.getDefaultValue().equals(globalValue)) {
							text.append(new FormattedText("cmd.messcfg.modified", "cl").asMutableText());
						}
						
						text.append(new FormattedText("cmd.messcfg.default", "f", true, opt.getDefaultValue()).asMutableText());
						ct.getSource().sendFeedback(text, false);
						return Command.SINGLE_SUCCESS;
					})
					.then(argument("value", StringArgumentType.greedyString())
							.suggests(sp)
							.executes((ct) -> {
								if(checkMixins(ct, name)) {
									String value = StringArgumentType.getString(ct, "value");
									if(opt.globalOnly()) {
										MutableText errMsg = new LiteralText(I18N.translate("cmd.messcfg.globalonly", name))
												.fillStyle(Style.EMPTY
														.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
																"/messcfg setGlobal " + name + ' ' + value)));
										ct.getSource().sendError(errMsg);
										return -1;
									}
									
									try {
										OptionManager.getActiveOptionSet().set(name, value, ct);
										CommandUtil.feedbackWithArgs(ct, "cmd.messcfg.set", name, value);
										return Command.SINGLE_SUCCESS;
									} catch (InvalidOptionException e) {
										CommandUtil.error(ct, e.getMessage());
										return -1;
									}
								} else {
									return 0;
								}
							})));
			command.then(literal("setGlobal").requires(CommandUtil.COMMAND_REQUMENT)
					.then(literal(name).requires(CommandUtil.COMMAND_REQUMENT)
							.then(argument("value", StringArgumentType.greedyString())
									.suggests(sp)
									.executes((ct) -> {
										if(checkMixins(ct, name)) {
											try {
												String value = StringArgumentType.getString(ct, "value");
												OptionManager.getGlobalOptionSet().set(name, value, ct);
												CommandUtil.feedbackWithArgs(ct, "cmd.messcfg.setglobal", name, value);
												return Command.SINGLE_SUCCESS;
											} catch (InvalidOptionException e) {
												e.printStackTrace();
												CommandUtil.error(ct, e.getMessage());
												return -1;
											}
										} else {
											return 0;
										}
									}))));
		});
		dispatcher.register(command);
	}
	
	private static void dumpOption(ServerCommandSource source, String name, OptionWrapper opt) {
		String v = OptionManager.getActiveOptionSet().getSerialized(name);
		ClickEvent event = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/messcfg " + name);
		MutableText text = new LiteralText(name + ": " + v)
				.fillStyle(Style.EMPTY.withClickEvent(event)
						.withHoverEvent((new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
								new LiteralText(opt.getDescription())))))
				.formatted(Formatting.GRAY);
		boolean modified = !v.equals(opt.getDefaultValue());
		source.sendFeedback(modified 
				? text.append(new FormattedText("cmd.messcfg.modified", "cl").asMutableText()) : text, false);
	}
	
	private static boolean checkMixins(CommandContext<ServerCommandSource> ct, String name) {
		if(MessModMixinPlugin.isFeatureAvailable(name)) {
			return true;
		} else {
			for(String mixin : MessModMixinPlugin.getAbsentMixins(name)) {
				CommandUtil.errorWithArgs(ct, "cmd.general.reqmixin", name, mixin);
			}
			
			return false;
		}
	}
}
