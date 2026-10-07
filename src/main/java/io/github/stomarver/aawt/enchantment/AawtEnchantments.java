package io.github.stomarver.aawt.enchantment;

import io.github.stomarver.aawt.Aawt;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The two enchantments this mod adds, defined in
 * {@code aawt_data/data/aawt/enchantment/*.json}.
 *
 * <p>That folder is served by
 * {@link io.github.stomarver.aawt.pack.ConditionalEnchantmentPack}, so the {@code beheading} and
 * {@code cleaving} config entries decide whether the definitions reach the registry at all: an
 * enchantment that is turned off is not registered, and {@link #level} then reports 0 for it because
 * the registry lookup finds nothing.
 *
 * <p>Their numbers (levels, costs, weight, applicable items) are data, so a pack can retune them
 * without code. Their <i>behaviour</i> is code - {@link Beheading} and {@link Cleaving} - because
 * vanilla has no enchantment effect type for "drop the victim's head" or for "let part of a hit
 * through a shield that is being broken"; see those classes for the exact rules.
 */
public final class AawtEnchantments {
	/** III; a mob or player killed with the axe may drop its head. */
	public static final ResourceKey<Enchantment> BEHEADING = ResourceKey.create(Registries.ENCHANTMENT, Aawt.id("beheading"));

	/** V; the hit that breaks a shield ignores part of its protection and disables it for longer. */
	public static final ResourceKey<Enchantment> CLEAVING = ResourceKey.create(Registries.ENCHANTMENT, Aawt.id("cleaving"));

	private AawtEnchantments() {
	}

	/**
	 * The level of one of this mod's enchantments on a stack, or 0.
	 *
	 * <p>Goes through the registry every time instead of caching a {@link net.minecraft.core.Holder}:
	 * holders belong to a registry instance, and a data pack reload replaces it.
	 */
	public static int level(RegistryAccess access, ItemStack stack, ResourceKey<Enchantment> enchantment) {
		if (stack.isEmpty()) {
			return 0;
		}

		Registry<Enchantment> registry = access.lookupOrThrow(Registries.ENCHANTMENT);

		return registry.get(enchantment).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
	}
}
