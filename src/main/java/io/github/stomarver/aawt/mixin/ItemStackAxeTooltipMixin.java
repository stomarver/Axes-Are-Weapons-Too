package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.knockback.KnockbackSuppression;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Adds the knockback suppression line to the tooltip of every axe.
 *
 * <p>It is appended at the end of {@code ItemStack#addAttributeTooltips}, i.e. right below the
 * "When in Main Hand:" block, so that the line reads as one more property of holding the axe -
 * which is accurate: {@code LivingEntity#getWeaponItem()} is the main hand stack.
 *
 * <p>This mixin lives in the common config on purpose: {@code ItemStack} and everything the
 * injection touches exist in both the client and the server jar, and building tooltip lines is
 * side independent.
 *
 * <p>Whether the line appears is decided by the server: {@code KnockbackSuppression.isActive()}
 * reads {@code EffectiveSettings}, which on a client is the mirror of what the server sent when the
 * player joined. No mod on the server - no payload - no line, whatever the client's own config says.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAxeTooltipMixin {
	@Inject(method = "addAttributeTooltips", at = @At("TAIL"))
	private void aawt$appendKnockbackSuppression(Consumer<Component> lines, TooltipDisplay display, Player player, CallbackInfo ci) {
		ItemStack self = (ItemStack) (Object) this;

		if (!KnockbackSuppression.isActive() || !self.is(ItemTags.AXES) || !display.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
			return;
		}

		// An axe normally has attack damage/speed modifiers, so vanilla has already printed the
		// header. A tag-only axe without any modifier of its own still gets a proper section.
		if (!aawt$hasVisibleMainHandModifier(self)) {
			lines.accept(CommonComponents.EMPTY);
			lines.accept(Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY));
		}

		lines.accept(KnockbackSuppression.tooltipLine());
	}

	/** Mirrors the check vanilla does before printing the "When in Main Hand:" header. */
	@Unique
	private static boolean aawt$hasVisibleMainHandModifier(ItemStack stack) {
		boolean[] visible = new boolean[1];

		stack.forEachModifier(
				EquipmentSlotGroup.MAINHAND,
				(attribute, modifier, modifierDisplay) -> visible[0] |= modifierDisplay != ItemAttributeModifiers.Display.hidden()
		);

		return visible[0];
	}
}
