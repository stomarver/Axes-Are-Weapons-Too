package io.github.stomarver.aawt.client;

import io.github.stomarver.aawt.Aawt;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. Everything this mod does on a client is about <i>following the server</i>:
 * there is no client-side mechanic, only a mirror of the values the server plays with - plus the way
 * the config screen looks.
 */
public class AawtClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ServerSettingsMirror.install();
		registerConfigScreen();
	}

	/**
	 * Registers the config again, this time as the client flavour of the same class: same entries,
	 * same file, but {@code getScreen} now returns {@link AawtConfigScreen}, which draws an item
	 * in front of each label. Re-registering replaces the instance every call into the config screen
	 * asks, and MidnightConfig keeps its entries keyed by "modid:field", so nothing is duplicated.
	 * The common initializer has already run by now - client entrypoints always come after the main
	 * ones.
	 *
	 * <p>Wrapped because it is pure cosmetics: a failure here must not cost the player the client, and
	 * without it the config simply opens as the plain MidnightConfig screen the common initializer
	 * already registered.
	 */
	private static void registerConfigScreen() {
		try {
			MidnightConfig.init(Aawt.MOD_ID, AawtClientConfig.class);
		} catch (Throwable t) {
			Aawt.LOGGER.error("Config entry icons are off: the config screen stays the plain MidnightConfig one", t);
		}
	}
}
