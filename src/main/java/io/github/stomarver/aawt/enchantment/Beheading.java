package io.github.stomarver.aawt.enchantment;

import io.github.stomarver.aawt.AawtConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Beheading (Обезглавливание), level III: a mob, a player or a mannequin killed with the enchanted
 * axe may drop its head.
 *
 * <table>
 *   <caption>Chance to drop a head</caption>
 *   <tr><td>I</td><td>30%</td></tr>
 *   <tr><td>II</td><td>50%</td></tr>
 *   <tr><td>III</td><td>65%</td></tr>
 * </table>
 *
 * <p>The heads are the ones a charged creeper produces - skeleton skull, zombie head, creeper head,
 * piglin head, wither skeleton skull - plus the player head, which a charged creeper never leaves.
 *
 * <h2>How the drop is produced</h2>
 *
 * <p>Mobs are rolled through vanilla's own charged-creeper loot table
 * ({@link BuiltInLootTables#CHARGED_CREEPER}, i.e. {@code minecraft:charged_creeper/root}), which is
 * the same table {@code Creeper#killedEntity} uses: it dispatches on the victim's entity type to
 * {@code charged_creeper/zombie}, {@code .../skeleton}, {@code .../creeper}, {@code .../piglin},
 * {@code .../wither_skeleton} and yields exactly the right head. So there is no hand-written
 * entity-to-item map here, and if Mojang adds a head for a new mob, Beheading drops it too.
 *
 * <p>Players and mannequins are handled in code because that loot table has no entry for either, and
 * both carry a {@link ResolvableProfile} - {@code ServerPlayer#getGameProfile()} and
 * {@code Mannequin#getProfile()} - which is exactly what the {@code minecraft:profile} component of a
 * player head holds, so the skin comes along. A mannequin that was never given a skin keeps
 * {@link Mannequin#DEFAULT_PROFILE}, an empty profile; such a head is dropped without one.
 * {@code minecraft:mannequin} has an empty loot table of its own, so nothing else is added by the
 * death of a mannequin.
 *
 * <p>The name of a dropped player head follows {@code playerHeadNaming} in the config, and its
 * description line - the grey line under the name - follows {@code playerHeadLore}; both live in
 * {@link PlayerHeadNaming}.
 *
 * <p>Like the charged creeper, the roll happens inside {@code LivingEntity#dropAllDeathLoot} and is
 * gated by {@code LivingEntity#shouldDropLoot}, so {@code doMobLoot} and babies are respected.
 * Wither skeletons keep their vanilla 2.5% skull chance from their own loot table; Beheading adds an
 * independent roll on top, which is the point - it replaces the grind, not the loot table.
 *
 * <p>With {@code beheading} turned off in the config the enchantment is removed from the registry on
 * the next {@code /reload}, which already makes the level 0. The check below additionally stops the
 * effect immediately, before that reload happens.
 */
public final class Beheading {
	/** Chance to drop a head per level: I - 30%, II - 50%, III - 65%. */
	private static final float[] HEAD_DROP_CHANCE = {0.30F, 0.50F, 0.65F};

	private Beheading() {
	}

	/**
	 * Rolls the head drop for a victim that is about to leave its loot. Server-side only:
	 * {@code dropAllDeathLoot} runs inside {@code LivingEntity#die} on the server.
	 *
	 * @param victim the entity that died; its {@code shouldDropLoot} has already been checked
	 * @param level  the server level
	 * @param source damage source of the killing blow
	 */
	public static void rollHeadDrop(LivingEntity victim, ServerLevel level, DamageSource source) {
		if (!AawtConfig.beheading) {
			return;
		}

		// The killer has to be the one who hit: getDirectEntity rules out arrows, explosions and
		// anything else where the axe holder is only indirectly responsible.
		if (!(source.getDirectEntity() instanceof LivingEntity killer)) {
			return;
		}

		ItemStack weapon = killer.getWeaponItem();

		if (!weapon.is(ItemTags.AXES)) {
			return;
		}

		int enchantmentLevel = AawtEnchantments.level(level.registryAccess(), weapon, AawtEnchantments.BEHEADING);

		if (enchantmentLevel <= 0 || victim.getRandom().nextFloat() >= chance(enchantmentLevel)) {
			return;
		}

		if (victim instanceof ServerPlayer player) {
			dropPlayerHead(victim, level, ResolvableProfile.createResolved(player.getGameProfile()), killer);
		} else if (victim instanceof Mannequin mannequin) {
			dropPlayerHead(victim, level, mannequin.getProfile(), killer);
		} else {
			// Exactly what a charged creeper does, minus the creeper.
			victim.dropFromLootTable(level, source, false, BuiltInLootTables.CHARGED_CREEPER);
		}
	}

	/** Chance for a level of I..III, clamped so a data pack raising max_level cannot overflow it. */
	public static float chance(int level) {
		return HEAD_DROP_CHANCE[Math.min(Math.max(level, 1), HEAD_DROP_CHANCE.length) - 1];
	}

	/** Builds the head around a profile, names it per config and drops it where the victim died. */
	private static void dropPlayerHead(LivingEntity victim, ServerLevel level, ResolvableProfile profile, LivingEntity killer) {
		ItemStack head = new ItemStack(Items.PLAYER_HEAD);

		// An empty profile would turn the head into a stranger: a mannequin without a skin keeps its
		// DEFAULT_PROFILE, and such a head is dropped plain.
		if (profile != null && profile != Mannequin.DEFAULT_PROFILE) {
			head.set(DataComponents.PROFILE, profile);
		}

		Component name = PlayerHeadNaming.name(profile, victim, killer);

		if (name != null) {
			head.set(DataComponents.ITEM_NAME, name);
		}

		// One string travels on the head: the template with its placeholders filled in, empty when
		// the config asks for no line at all. Which language the translation keys left in it speak is
		// decided by the client that hovers the head, where vanilla would draw its "Dynamic" line.
		CompoundTag loreTag = new CompoundTag();
		loreTag.putString(PlayerHeadNaming.LORE_LINE_TAG, PlayerHeadNaming.loreLine(profile, victim, killer));
		head.set(DataComponents.CUSTOM_DATA, CustomData.of(loreTag));

		victim.spawnAtLocation(level, head);
	}
}
