package lovexyn0827.mess.network;

import net.minecraft.util.Identifier;

public interface Channels {
	public static int CHANNEL_VERSION = 9;	// TODO Remember to update the channel version if necessary
	Identifier SHAPE = new Identifier("messmod", "shape");
	Identifier VERSION = new Identifier("messmod", "version");
	Identifier OPTIONS = new Identifier("messmod", "options");
	Identifier OPTION_SINGLE = new Identifier("messmod", "option_single");
}
