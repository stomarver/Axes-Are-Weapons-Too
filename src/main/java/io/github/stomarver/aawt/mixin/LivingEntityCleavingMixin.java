package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.enchantment.Cleaving;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cleaving: the hit that breaks a shield ignores part of its protection and disables it longer.
 *
 * <p>Both injections sit on {@code RETURN} and only ever change a value vanilla has already decided
 * to produce - the shield cooldown of the attacker's weapon and the damage the defender's shield
 * blocked. Neither creates a new code path, and both are no-ops without the enchantment.
 * See {@link Cleaving} for the exact conditions.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCleavingMixin {
	@Inject(method = "getSecondsToDisableBlocking", at = @At("RETURN"), cancellable = true)
	private void aawt$extendShieldDisable(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(Cleaving.extendDisableSeconds((LivingEntity) (Object) this, cir.getReturnValueF()));
	}

	@Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
	private void aawt$cleavingIgnoresShieldProtection(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		float blocked = cir.getReturnValueF();

		if (blocked > 0.0F) {
			cir.setReturnValue(Cleaving.reduceBlockedDamage((LivingEntity) (Object) this, source, blocked));
		}
	}
}
