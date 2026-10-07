package io.github.stomarver.aawt.sync;

import io.github.stomarver.aawt.Aawt;
import io.github.stomarver.aawt.AawtConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * What a server runs with, sent to its clients - and at the same time the plain record of
 * "the values in force right now" that every mechanic reads.
 *
 * <p>{@code knockbackSuppression} is the configured share of knockback an axe hit removes.
 * {@code axeEnchantmentSplit} is not configurable: it says whether the server runs the mod's
 * enchantment split at all, which is another way of saying "the server has this mod". A client
 * needs to know that, because enchantment compatibility is checked on both sides and the client
 * has to predict the same answer the server will give.
 *
 * <p>Traffic is one-way: nothing is ever sent to the server.
 */
public record SettingsPayload(double knockbackSuppression, boolean axeEnchantmentSplit) implements CustomPacketPayload {
	public static final Identifier ID = Aawt.id("settings");
	public static final Type<SettingsPayload> TYPE = new Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, SettingsPayload::knockbackSuppression,
			ByteBufCodecs.BOOL, SettingsPayload::axeEnchantmentSplit,
			SettingsPayload::new);

	/** What a client plays by while no server has told it anything: the mod is not there. */
	public static final SettingsPayload DISABLED = new SettingsPayload(0.0, false);

	/**
	 * Values that arrive over the network are never trusted blindly: a share outside 0..1
	 * (including NaN) would be nonsense, so the record clamps it in both directions.
	 */
	public SettingsPayload {
		knockbackSuppression = Double.isFinite(knockbackSuppression) ? Mth.clamp(knockbackSuppression, 0.0, 1.0) : 0.0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** A snapshot of the config of the side that calls this - on a server, the authority. */
	public static SettingsPayload fromConfig() {
		return new SettingsPayload(AawtConfig.knockbackSuppression, true);
	}
}
