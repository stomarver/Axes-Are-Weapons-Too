package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.enchantment.Beheading;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the victim's head when it was killed with a Beheading axe.
 *
 * <p>{@code dropAllDeathLoot} is called from {@code LivingEntity#die} (and from {@code ServerPlayer#die})
 * on the server, right where vanilla itself drops loot - including the head a charged creeper kills
 * with. Injecting at the head of the method and checking {@code shouldDropLoot} reuses vanilla's own
 * gate, so {@code doMobLoot} and babies behave exactly as they do for every other drop.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBeheadingMixin {
	@Shadow
	protected abstract boolean shouldDropLoot(ServerLevel level);

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void aawt$beheading(ServerLevel level, DamageSource source, CallbackInfo ci) {
		LivingEntity victim = (LivingEntity) (Object) this;

		if (this.shouldDropLoot(level)) {
			Beheading.rollHeadDrop(victim, level, source);
		}
	}
}
