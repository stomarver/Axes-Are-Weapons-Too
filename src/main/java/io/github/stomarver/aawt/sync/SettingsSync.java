package io.github.stomarver.aawt.sync;

import io.github.stomarver.aawt.Aawt;
import io.github.stomarver.aawt.AawtConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server half of the settings sync: tells every client what the server is running with.
 *
 * <p>Registration happens in the common initializer because both sides need to know the payload
 * type; the client registers its receiver in {@code src/client} ({@code ServerSettingsMirror}).
 *
 * <p>A player receives the settings when they join, and everybody receives them again if the
 * server's value changes while the world is running - an admin editing
 * {@code config/aawt.json} or using MidnightLib's {@code /midnightconfig aawt} command.
 * Clients never have to rejoin to learn what the server decided.
 *
 * <p>A client without this mod is skipped: {@code ServerPlayNetworking.canSend} is only true for
 * clients that declared during the configuration phase that they can receive this payload.
 */
public final class SettingsSync {
	// Last value handed out, so a change can be spotted without allocating anything per tick.
	private static double sentKnockbackSuppression = -1.0;

	private SettingsSync() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, SettingsPayload.TYPE)) {
				sender.sendPacket(SettingsPayload.fromConfig());
			}
		});

		// Adopt the current value as "already sent", so the first tick of a fresh server is not
		// mistaken for a change.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> sentKnockbackSuppression = AawtConfig.knockbackSuppression);

		ServerTickEvents.END_SERVER_TICK.register(SettingsSync::broadcastIfConfigChanged);
	}

	private static void broadcastIfConfigChanged(MinecraftServer server) {
		if (AawtConfig.knockbackSuppression == sentKnockbackSuppression) {
			return;
		}

		sentKnockbackSuppression = AawtConfig.knockbackSuppression;

		SettingsPayload settings = SettingsPayload.fromConfig();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, SettingsPayload.TYPE)) {
				ServerPlayNetworking.send(player, settings);
			}
		}

		Aawt.LOGGER.info("Knockback suppression is now {}, clients were notified", settings.knockbackSuppression());
	}
}
