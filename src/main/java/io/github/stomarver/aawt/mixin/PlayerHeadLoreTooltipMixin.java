package io.github.stomarver.aawt.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.stomarver.aawt.enchantment.PlayerHeadNaming;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the head description of {@code playerHeadLore} where vanilla draws its profile line.
 *
 * <p>An unresolved {@code minecraft:profile} puts a grey line reading
 * {@code component.profile.dynamic} - "Dynamic", in Russian "Динамический" - right under the item
 * name, and that is exactly the slot the description of a beheaded head belongs to: first line
 * under the name, grey, not italic. So on a head that carries the line {@link PlayerHeadNaming}
 * wrote into {@code minecraft:custom_data}, the vanilla line is dropped and the description takes
 * its place; every other stack - including a head whose lore entry is empty - keeps vanilla's
 * tooltip untouched.
 *
 * <p>Translation keys left in the stored line are turned into text here, on the client that draws
 * it, so the same head reads "Отсечена игроком X" for a Russian client and "Chopped by X" for an
 * English one, with no separator added anywhere: the spacing is the template's own.
 *
 * <p>{@code getTooltipLines} builds a fresh list per call, but the list is wrapped into a new one
 * anyway: nothing here may depend on how mutable vanilla's answer happens to be.
 */
@Mixin(ItemStack.class)
public abstract class PlayerHeadLoreTooltipMixin {
	/** Translation key of the vanilla line this mod replaces: "Dynamic" / "Динамический". */
	@Unique
	private static final String AAWT_DYNAMIC_LINE = "component.profile.dynamic";

	@ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
	private List<Component> aawt$putLoreInPlaceOfTheDynamicLine(List<Component> lines) {
		CustomData customData = ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);

		if (customData == null) {
			return lines;
		}

		CompoundTag tag = customData.copyTag();

		if (!tag.contains(PlayerHeadNaming.LORE_LINE_TAG)) {
			return lines;
		}

		String stored = tag.getString(PlayerHeadNaming.LORE_LINE_TAG).orElse("");
		Component lore = stored.isEmpty() ? null : PlayerHeadNaming.parseLoreLine(stored);

		List<Component> result = new ArrayList<>(lines.size() + 1);

		// An empty stored line - the config held a lone dot - silences the slot altogether: the
		// vanilla line is dropped and nothing takes its place.
		boolean placed = lore == null;

		for (Component line : lines) {
			if (line.getContents() instanceof TranslatableContents contents && AAWT_DYNAMIC_LINE.equals(contents.getKey())) {
				// The vanilla line gives up its slot: to the description, or to nothing at all.
				if (!placed) {
					result.add(lore);
					placed = true;
				}

				continue;
			}

			result.add(line);
		}

		if (!placed) {
			// A resolved profile draws no "Dynamic" line, so the description opens the tooltip body.
			result.add(Math.min(1, result.size()), lore);
		}

		return result;
	}
}
