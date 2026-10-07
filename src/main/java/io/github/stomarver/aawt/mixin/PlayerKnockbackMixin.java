package io.github.stomarver.aawt.mixin;

import io.github.stomarver.aawt.knockback.KnockbackSuppression;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Suppresses the extra knockback of a player's hit: the Knockback enchantment and the
 * {@code +0.5} of a sprint hit, both dispatched through {@code Player#causeExtraKnockback}.
 *
 * <p>Only the {@code LivingEntity#knockback} branch is touched. Non-living targets (boats,
 * minecarts) are pushed instead and are deliberately left alone: they have no attributes, so
 * knockback resistance never affected them in vanilla either.
 *
 * <p>Like the default knockback, this path is server-only: {@code Player#attack} calls it when
 * {@code Entity#hurtOrSimulate} succeeded, and on a client that always goes to
 * {@code Entity#hurtClient}, which returns false. The client never applies knockback itself, it
 * receives it.
 */
@Mixin(Player.class)
public abstract class PlayerKnockbackMixin {
	@ModifyArgs(
			method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V"
			)
	)
	private void aawt$suppressExtraKnockback(Args args) {
		double power = args.get(0);
		DamageSource source = args.get(3);

		args.set(0, KnockbackSuppression.apply(power, source));
	}
}
