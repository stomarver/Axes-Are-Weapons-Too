package io.github.stomarver.aawt.enchantment;

import io.github.stomarver.aawt.Aawt;
import io.github.stomarver.aawt.EffectiveSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The axe enchantment split of the rework: an axe is a weapon <i>or</i> a tool, never both.
 *
 * <p>The wiki divides the enchantments an axe can carry into two groups:
 *
 * <ul>
 *   <li><b>weapon</b> - Sharpness, Smite, Bane of Arthropods, Cleaving;</li>
 *   <li><b>tool</b> - Efficiency, Fortune, Silk Touch.</li>
 * </ul>
 *
 * <p>An axe may only hold enchantments of one group. Everything else keeps working as in vanilla:
 * conflicts <i>inside</i> a group stay exactly as they were (Sharpness/Smite/Bane of Arthropods are
 * mutually exclusive through {@code #minecraft:exclusive_set/damage}, Fortune/Silk Touch through
 * {@code #minecraft:exclusive_set/mining}, Efficiency conflicts with nothing, Cleaving with
 * nothing), and enchantments of neither group - Beheading, Unbreaking, Mending, Curse of Vanishing -
 * can still be added to any axe.
 *
 * <h2>Why "cross-group = incompatible" is the whole rule</h2>
 *
 * <p>{@code Enchantment.areCompatible} has no item parameter, so the rule cannot say "on an axe".
 * It does not need to: resolving {@code supported_items} of the vanilla enchantments of both groups
 * in 26.3 shows that
 *
 * <pre>
 *   #enchantable/weapon = #enchantable/sharp_weapon + mace = #enchantable/melee_weapon + #axes + mace
 *   #enchantable/mining = #axes + #pickaxes + #shovels + #hoes + shears
 *   weapon group ∩ tool group = the seven axes, and nothing else
 * </pre>
 *
 * i.e. the only items that can carry a weapon-group and a tool-group enchantment at the same time
 * are axes, so a global cross-group conflict is exactly the wiki's axe rule for every vanilla item.
 * Swords, spears and the mace cannot hold Efficiency/Fortune/Silk Touch in the first place, and
 * pickaxes, shovels, hoes and shears cannot hold Sharpness/Smite/Bane of Arthropods. Cleaving joins
 * the weapon group with {@code supported_items: #minecraft:axes}, which keeps that intersection
 * exactly as it is.
 *
 * <p>A modded item that deliberately supports both groups (an "axe-sword", say) inherits the split
 * as well. That is intended - and a pack that disagrees can edit the two tags below, because the
 * groups are plain data, not code.
 *
 * <h2>Where the rule is enforced</h2>
 *
 * <p>In {@code EnchantmentMixin}, on {@code Enchantment.areCompatible}. That single static method is
 * the only compatibility gate the game has, and everything funnels through it:
 *
 * <ul>
 *   <li>the anvil ({@code AnvilMenu#createResult}) refuses to merge the two groups and raises the cost;</li>
 *   <li>the enchanting table ({@code EnchantmentHelper.selectEnchantment} -&gt;
 *       {@code filterCompatibleEnchantments}) rolls one group and then drops the other from the
 *       bonus enchantments;</li>
 *   <li>{@code /enchant} ({@code EnchantmentHelper.isEnchantmentCompatible}) rejects the mix.</li>
 * </ul>
 *
 * <p>Nothing is overridden in vanilla data: no {@code exclusive_set} tag and no vanilla enchantment
 * JSON is touched, so the rule lives entirely in these two tags and in one injector.
 *
 * @see <a href="https://minecraft.wiki/w/Axe_changes">Minecraft Wiki: Axe changes</a>
 */
public final class AxeEnchantmentGroups {
	/** {@code data/aawt/tags/enchantment/weapon.json} */
	public static final TagKey<Enchantment> WEAPON = TagKey.create(Registries.ENCHANTMENT, Aawt.id("weapon"));

	/** {@code data/aawt/tags/enchantment/tool.json} */
	public static final TagKey<Enchantment> TOOL = TagKey.create(Registries.ENCHANTMENT, Aawt.id("tool"));

	private AxeEnchantmentGroups() {
	}

	/**
	 * Whether the split forbids these two enchantments from sharing an item. Only ever answers
	 * "incompatible"; it never makes vanilla-incompatible enchantments compatible.
	 *
	 * <p>The split is not configurable, but it does depend on where the code runs: the check goes
	 * through {@link EffectiveSettings}, so a client answers with the server's decision. That keeps
	 * the anvil preview in agreement with the result the server will produce, and it is what makes a
	 * server without this mod a vanilla server for its clients.
	 */
	public static boolean areIncompatible(Holder<Enchantment> first, Holder<Enchantment> second) {
		if (!EffectiveSettings.get().axeEnchantmentSplit()) {
			return false;
		}

		return separates(first, second) || separates(second, first);
	}

	/** True when {@code weaponSide} belongs to the weapon group and {@code toolSide} to the tool group. */
	private static boolean separates(Holder<Enchantment> weaponSide, Holder<Enchantment> toolSide) {
		return weaponSide.is(WEAPON) && toolSide.is(TOOL);
	}
}
