package io.github.stomarver.aawt.client;

import io.github.stomarver.aawt.EffectiveSettings;
import io.github.stomarver.aawt.sync.SettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;

/**
 * The client's copy of the settings the server runs with - the only values a client plays by.
 *
 * <p>Filled by the {@code aawt:settings} payload the server sends when the player joins (and
 * again whenever the server's config changes), cleared as soon as a connection starts or ends:
 *
 * <ul>
 *   <li><b>server has the mod</b> - the payload arrives and the mirror holds the server's values:
 *       the tooltip, the enchantment split and everything that follows behave as the server wants;</li>
 *   <li><b>server does not have the mod</b> - nothing arrives, the mirror stays empty and
 *       {@link EffectiveSettings} hands out {@link SettingsPayload#DISABLED}: no tooltip, no
 *       enchantment split, no enchantment of this mod. The client's own config file is ignored on
 *       purpose;</li>
 *   <li><b>no world</b> (main menu) - also empty, because there is no authority to ask.</li>
 * </ul>
 *
 * <p>Resetting on {@code INIT} and not only on {@code DISCONNECT} is deliberate: {@code INIT} fires
 * from the {@code ClientPacketListener} constructor, before any play payload of the new connection
 * can be processed, so a stale value from a previous server cannot survive into the next one even if
 * a disconnect event were skipped.
 */
public final class ServerSettingsMirror {
	/**
	 * Written on the network thread by the payload receiver, read on the render thread by tooltips
	 * and by the enchantment compatibility check - hence volatile, and hence an immutable record.
	 */
	@Nullable
	private static volatile SettingsPayload fromServer;

	private ServerSettingsMirror() {
	}

	public static void install() {
		EffectiveSettings.installClientMirror(ServerSettingsMirror::settings);

		ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> fromServer = payload);

		ClientPlayConnectionEvents.INIT.register((handler, client) -> fromServer = null);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> fromServer = null);
	}

	/** The server's settings, or "the mod is not installed here" while there are none. */
	private static SettingsPayload settings() {
		SettingsPayload payload = fromServer;

		return payload != null ? payload : SettingsPayload.DISABLED;
	}
}
