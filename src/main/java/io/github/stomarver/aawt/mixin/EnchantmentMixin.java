package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.enchantment.AxeEnchantmentGroups;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the axe enchantment split to the game's only compatibility gate.
 *
 * <p>{@code Enchantment.areCompatible} is what the anvil, the enchanting table and {@code /enchant}
 * all ask before putting two enchantments on one item; vanilla implements it purely through the
 * {@code exclusive_set} tags. Injecting here means one hook covers every path, including mods that
 * reuse the vanilla helper, and no vanilla data file has to be overwritten.
 *
 * <p>The injection sits at {@code HEAD} and can only turn "compatible" into "incompatible": the
 * cross-group pairs it rejects are pairs vanilla would have accepted, and everything vanilla
 * rejects (same enchantment, {@code exclusive_set}) is left to vanilla. See
 * {@link AxeEnchantmentGroups} for why a rule without an item parameter is enough.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
	private static void aawt$splitAxeEnchantmentGroups(Holder<Enchantment> first, Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
		if (AxeEnchantmentGroups.areIncompatible(first, second)) {
			cir.setReturnValue(false);
		}
	}
}
