package io.github.stomarver.aawt.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets an item name that was set on purpose win on a player head.
 *
 * <p>Vanilla's {@code PlayerHeadItem#getName} derives "%s's Head" from the {@code minecraft:profile}
 * component and only falls back to the item's own name when the profile has none, so a
 * {@code minecraft:item_name} that something wrote onto the head - which is how
 * {@link io.github.stomarver.aawt.enchantment.PlayerHeadNaming} names a beheaded player or mannequin -
 * would never be shown. This injects in front of that and returns the item name whenever it differs
 * from the default one the item was built with, i.e. whenever it was set explicitly. Heads that
 * nobody named keep vanilla's behaviour exactly.
 *
 * <p>An anvil rename is untouched: that writes {@code minecraft:custom_name}, which
 * {@code ItemStack#getHoverName} already prefers over the item name.
 */
@Mixin(PlayerHeadItem.class)
public abstract class PlayerHeadItemMixin {
	/** Default {@code minecraft:item_name} per item, so no stack has to be built on every tooltip. */
	@Unique
	private static final Map<Item, Component> aawt$defaultItemNames = new ConcurrentHashMap<>();

	@Inject(method = "getName", at = @At("HEAD"), cancellable = true)
	private void aawt$preferExplicitItemName(ItemStack stack, CallbackInfoReturnable<Component> info) {
		Component name = stack.get(DataComponents.ITEM_NAME);

		if (name != null && !name.equals(aawt$defaultItemName(stack.getItem()))) {
			info.setReturnValue(name);
		}
	}

	@Unique
	private static Component aawt$defaultItemName(Item item) {
		return aawt$defaultItemNames.computeIfAbsent(item, built -> {
			Component name = new ItemStack(built).get(DataComponents.ITEM_NAME);

			return name == null ? CommonComponents.EMPTY : name;
		});
	}
}
