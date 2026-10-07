package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.pack.ConditionalEnchantmentPack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hands {@link ConditionalEnchantmentPack} to whoever loads the selected packs, which is how the two
 * config toggles reach the enchantment registry.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
	/**
	 * Called by the server when it loads data packs (start-up and {@code /reload}) and by the client
	 * when it loads resource packs; the pack itself answers nothing for client resources.
	 */
	@ModifyReturnValue(method = "openAllSelected", at = @At("RETURN"))
	private List<PackResources> aawt$conditionalEnchantments(List<PackResources> packs) {
		Optional<PackResources> conditional = ConditionalEnchantmentPack.get();

		if (conditional.isEmpty()) {
			return packs;
		}

		List<PackResources> all = new ArrayList<>(packs.size() + 1);

		// First in the list is the lowest priority, so a real data pack still wins.
		all.add(conditional.get());
		all.addAll(packs);

		return List.copyOf(all);
	}
}
