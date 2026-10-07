package io.github.stomarver.aawt.knockback;

import io.github.stomarver.aawt.EffectiveSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The whole "knockback suppression" rule of an axe in one place, so that the mechanic
 * (mixins into the knockback pipeline) and its tooltip always describe the same thing.
 *
 * <p>An axe hit is treated as if the target had {@link #amount()} more knockback resistance:
 * the knockback power is multiplied by {@code 1 - amount}, exactly like vanilla multiplies it
 * by {@code 1 - KNOCKBACK_RESISTANCE} inside {@code LivingEntity#knockback}. Because it is applied
 * to the power of the hit and not to the attacker, it does not care about enchantments: the extra
 * knockback of Knockback or of a sprint hit is reduced by the same share.
 *
 * <p>Both injection points are server-only code: {@code LivingEntity#dealDefaultKnockback} is
 * reached from {@code LivingEntity#hurtServer}, and {@code Player#attack} only calls
 * {@code causeExtraKnockback} when {@code Entity#hurtOrSimulate} returned true, which on a client
 * it never does ({@code Entity#hurtClient} returns false). The mechanic is therefore server-side by
 * construction - a client cannot apply it, fake it or opt out of it.
 *
 * <p>The number comes from {@link EffectiveSettings}, i.e. from the server.
 */
public final class KnockbackSuppression {
	/** Translation key of the name shown in the tooltip, after the "+4" part. */
	public static final String NAME_KEY = "aawt.attribute.knockback_suppression";

	/** Vanilla's own attribute line format: {@code attribute.modifier.plus.0} = "+%s %s". */
	private static final String PLUS_FORMAT_KEY = "attribute.modifier.plus.0";

	private KnockbackSuppression() {
	}

	/** Whether axe hits currently remove any knockback at all. */
	public static boolean isActive() {
		return amount() > 0.0;
	}

	/** The share of knockback that is removed, clamped to {@code 0.0..1.0}. */
	public static double amount() {
		return EffectiveSettings.get().knockbackSuppression();
	}

	/**
	 * Scales the knockback power of a hit down if it was dealt by a player swinging an axe.
	 *
	 * @param power  knockback power vanilla is about to apply
	 * @param source damage source of the hit
	 * @return the power the target should actually receive
	 */
	public static double apply(double power, DamageSource source) {
		if (!isActive() || !isAxeHit(source)) {
			return power;
		}

		return power * (1.0 - amount());
	}

	/**
	 * The tooltip line, formatted the way vanilla formats an attribute bonus: "+4 Knockback
	 * Suppression" in English, "+4 Подавление отбрасывания" in Russian.
	 *
	 * <p>The number is the configured share times ten and the line is blue, so a default axe reads
	 * like an attribute bonus of a piece of netherite armour would: 0.4 -&gt; "+4".
	 */
	public static Component tooltipLine() {
		return Component.translatable(
						PLUS_FORMAT_KEY,
						ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount() * 10.0),
						Component.translatable(NAME_KEY))
				.withStyle(ChatFormatting.BLUE);
	}

	/**
	 * A melee hit of a living attacker holding an axe in the main hand.
	 *
	 * <p>{@code player_attack} plus the attacker being the direct cause rules out everything that
	 * merely involves a player with an axe in hand: arrows, thrown tridents, explosions, thorns.
	 * {@link Player#getWeaponItem()} is the main hand stack, which is also what {@code Player#attack}
	 * swings with. Items are matched through the {@code minecraft:axes} tag, so modded axes count too.
	 */
	private static boolean isAxeHit(DamageSource source) {
		if (!source.is(DamageTypes.PLAYER_ATTACK)) {
			return false;
		}

		return source.getDirectEntity() instanceof Player player && player.getWeaponItem().is(ItemTags.AXES);
	}
}
