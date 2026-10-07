package io.github.stomarver.aawt.enchantment;

import io.github.stomarver.aawt.AawtConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Cleaving (Прорубание), level V: the hit that breaks a shield ignores part of the shield's
 * protection and disables it for longer.
 *
 * <table>
 *   <caption>Per level</caption>
 *   <tr><td>Level</td><td>Protection ignored</td><td>Shield cooldown</td></tr>
 *   <tr><td>I</td><td>20%</td><td>5.2 s</td></tr>
 *   <tr><td>II</td><td>40%</td><td>5.4 s</td></tr>
 *   <tr><td>III</td><td>60%</td><td>5.6 s</td></tr>
 *   <tr><td>IV</td><td>80%</td><td>5.8 s</td></tr>
 *   <tr><td>V</td><td>100%</td><td>6.0 s</td></tr>
 * </table>
 *
 * <p>Only the breaking hit is affected - the one that vanilla would have blocked completely.
 *
 * <h2>Where vanilla does what</h2>
 *
 * <pre>
 * LivingEntity#hurtServer
 *   damageBlocked = applyItemBlocking(level, source, damage)     // BlocksAttacks.resolveBlockedDamage
 *   ...  if (damageBlocked &gt; 0 &amp;&amp; !projectile &amp;&amp; directEntity instanceof LivingEntity)
 *          blockUsingItem(level, attacker, source, damage, damageBlocked &gt;= damage)
 *   damage -= damageBlocked
 *
 * Player#blockUsingItem                                          // only players disable their shield
 *   seconds = attacker.getSecondsToDisableBlocking()             // Weapon.disableBlockingForSeconds, 5 for an axe
 *   if (seconds &gt; 0) blocksAttacks.disable(level, this, seconds, shield)
 * </pre>
 *
 * <p>So the mod touches exactly two numbers, both on the attacker's side:
 *
 * <ul>
 *   <li>{@code LivingEntity#getSecondsToDisableBlocking} is extended by 0.2 s per level - that value
 *       is what {@code Player#blockUsingItem} feeds into {@code BlocksAttacks#disable}, so the
 *       cooldown becomes 5.2 ... 6.0 s. The Warden overrides the method and is unaffected;</li>
 *   <li>the blocked amount returned by {@code applyItemBlocking} is scaled by
 *       {@code 1 - 0.2 * level}, so {@code damage -= damageBlocked} leaves 20-100% of the hit.
 *       Everything inside {@code applyItemBlocking} still uses the unmodified amount: the shield
 *       takes its normal durability damage and {@code fullyBlocked} stays true, which means the
 *       shield <i>is</i> disabled even at level V, when nothing is blocked any more.</li>
 * </ul>
 *
 * <p>Server-side only: {@code applyItemBlocking} takes a {@code ServerLevel}, and the shield cooldown
 * is applied by the server. Cleaving is a weapon-group enchantment (see {@link AxeEnchantmentGroups}),
 * so it stays compatible with Sharpness, Smite and Bane of Arthropods and cannot share an axe with
 * Efficiency, Fortune or Silk Touch.
 *
 * <p>With {@code cleaving} turned off in the config the enchantment is removed from the registry on
 * the next {@code /reload}, which already makes the level 0. The check in {@link #level} additionally
 * stops the effect immediately, before that reload happens.
 */
public final class Cleaving {
	/** Share of the shield's protection ignored per level: 20% at I, 100% at V. */
	public static final float PROTECTION_IGNORED_PER_LEVEL = 0.2F;

	/** Extra shield cooldown per level: 0.2 s, on top of the weapon's own 5 s. */
	public static final float EXTRA_DISABLE_SECONDS_PER_LEVEL = 0.2F;

	private Cleaving() {
	}

	/**
	 * Extends how long the attacker's weapon disables blocking.
	 *
	 * @param attacker the entity that is about to break a shield
	 * @param seconds  what vanilla's {@code getSecondsToDisableBlocking} produced - 0 for anything
	 *                 that does not break shields, in which case nothing is changed
	 */
	public static float extendDisableSeconds(LivingEntity attacker, float seconds) {
		if (seconds <= 0.0F) {
			return seconds;
		}

		return seconds + EXTRA_DISABLE_SECONDS_PER_LEVEL * level(attacker);
	}

	/**
	 * Reduces the damage a shield blocked, if this very hit breaks that shield.
	 *
	 * <p>The checks mirror the ones vanilla makes before disabling a shield, so the rule applies to
	 * breaking hits and to nothing else: something was blocked, the defender is a player (only
	 * {@code Player#blockUsingItem} disables), the hit is not a projectile, its direct cause is a
	 * living attacker, and that attacker's weapon actually breaks shields.
	 *
	 * @param defender the entity whose shield blocked the hit
	 * @param source   damage source of the hit
	 * @param blocked  amount of damage the shield blocked, as vanilla computed it
	 * @return the amount that should stay blocked
	 */
	public static float reduceBlockedDamage(LivingEntity defender, DamageSource source, float blocked) {
		if (blocked <= 0.0F || !(defender instanceof Player)) {
			return blocked;
		}

		if (source.is(DamageTypeTags.IS_PROJECTILE) || !(source.getDirectEntity() instanceof LivingEntity attacker)) {
			return blocked;
		}

		if (attacker.getSecondsToDisableBlocking() <= 0.0F) {
			return blocked;
		}

		return scaleBlocked(blocked, level(attacker));
	}

	/**
	 * The damage the shield is still allowed to block, with Cleaving of {@code level} hitting it.
	 * A fully blocked hit of 10 becomes 8 / 6 / 4 / 2 / 0 at levels I-V, i.e. the attacker's damage
	 * goes through as 20 / 40 / 60 / 80 / 100%.
	 *
	 * <p>Clamped, so a data pack raising {@code max_level} past V cannot turn a blocked hit into
	 * extra damage.
	 */
	public static float scaleBlocked(float blocked, int level) {
		float ignored = Math.min(PROTECTION_IGNORED_PER_LEVEL * level, 1.0F);

		return blocked * (1.0F - ignored);
	}

	/** Level on the attacker's axe, or 0 - both when there is none and when the config turned Cleaving off. */
	private static int level(LivingEntity attacker) {
		if (!AawtConfig.cleaving) {
			return 0;
		}

		ItemStack weapon = attacker.getWeaponItem();

		return AawtEnchantments.level(attacker.level().registryAccess(), weapon, AawtEnchantments.CLEAVING);
	}
}
