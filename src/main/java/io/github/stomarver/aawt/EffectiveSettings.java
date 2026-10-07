package io.github.stomarver.aawt;

import io.github.stomarver.aawt.sync.SettingsPayload;

import java.util.function.Supplier;

/**
 * The values the mod actually plays by - as opposed to the values sitting in a config file.
 *
 * <p><b>The server is authoritative.</b> Every mechanic of this mod runs in server-only code
 * (knockback, head drops, shield breaking) or has to agree with the server (enchantment
 * compatibility, tooltips), so:
 *
 * <ul>
 *   <li>on a <b>dedicated server</b> nothing is installed here and the mod reads its own
 *       {@code config/aawt.json} - that file <i>is</i> the authority;</li>
 *   <li>on a <b>client</b> the client entrypoint installs a mirror of what the server sent when the
 *       player joined. The client's own config file then has no effect: no tooltip, no knockback
 *       suppression, no enchantment split unless the server says so. A server without this mod sends
 *       nothing, the mirror stays empty and everything is {@link SettingsPayload#DISABLED}.</li>
 * </ul>
 *
 * <p>In singleplayer the integrated server reads the same file the client screen edits, so the two
 * agree - and the mirror is kept current while the world runs (see {@link
 * io.github.stomarver.aawt.sync.SettingsSync}).
 */
public final class EffectiveSettings {
	private static volatile Supplier<SettingsPayload> settingsSource = SettingsPayload::fromConfig;

	private EffectiveSettings() {
	}

	/**
	 * Replaces "read the local config file" with "read what the server told us".
	 * Called once from the client entrypoint; never called on a dedicated server.
	 */
	public static void installClientMirror(Supplier<SettingsPayload> settings) {
		settingsSource = settings;
	}

	/** The settings in force right now, on the side that is asking. */
	public static SettingsPayload get() {
		return settingsSource.get();
	}
}
