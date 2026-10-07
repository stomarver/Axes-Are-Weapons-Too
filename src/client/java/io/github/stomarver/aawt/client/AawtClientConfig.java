package io.github.stomarver.aawt.client;

import io.github.stomarver.aawt.Aawt;
import io.github.stomarver.aawt.AawtConfig;
import eu.midnightdust.lib.config.MidnightConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * {@link AawtConfig} as the client sees it: the same fields, the same
 * {@code config/aawt.json}, the same entries - only the screen it hands out differs.
 *
 * <p>{@code MidnightConfig#getScreen(Screen)} is MidnightLib's own extension point. Every way into
 * the config asks it, through {@code MidnightConfig.getScreen(parent, modid)}: Mod Menu's button,
 * MidnightLib's overview screen in the options menu, and anything else that follows the same call.
 * Overriding it here therefore covers all of them with one class, and without touching MidnightLib's
 * internals.
 *
 * <p>The override cannot live in {@code AawtConfig} itself: that class is common code, shared
 * with the dedicated server, and this screen is a client class. So the common initializer registers
 * {@code AawtConfig}, and {@link AawtClient} - which runs after it, on the client only -
 * registers this subclass in its place.
 *
 * <p>Icons are cosmetics, so the screen is built defensively: should a future MidnightLib move
 * something {@link AawtConfigScreen} relies on, the config still opens - the library's own
 * screen, without icons, and one error in the log.
 */
public class AawtClientConfig extends AawtConfig {
	@Override
	public MidnightConfigScreen getScreen(Screen parent) {
		try {
			return new AawtConfigScreen(parent, this.modid);
		} catch (Throwable t) {
			Aawt.LOGGER.error("Opening the plain MidnightConfig screen for {} instead", Aawt.MOD_ID, t);
			return super.getScreen(parent);
		}
	}
}
