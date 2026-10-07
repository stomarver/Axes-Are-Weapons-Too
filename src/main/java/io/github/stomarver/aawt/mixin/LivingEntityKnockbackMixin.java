package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.knockback.KnockbackSuppression;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Suppresses the knockback every hit deals by default.
 *
 * <p>{@code LivingEntity#dealDefaultKnockback} is where the base knockback of a damage source is
 * dispatched (vanilla passes a flat {@code 0.4} into {@code LivingEntity#knockback}, which then
 * multiplies it by {@code 1 - knockback resistance}). Reducing the power at this call means the
 * suppression stacks with the target's own resistance in exactly the same way an attribute would.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackMixin {
	@ModifyArgs(
			method = "dealDefaultKnockback(Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V"
			)
	)
	private void aawt$suppressDefaultKnockback(Args args) {
		double power = args.get(0);
		DamageSource source = args.get(3);

		args.set(0, KnockbackSuppression.apply(power, source));
	}
}
