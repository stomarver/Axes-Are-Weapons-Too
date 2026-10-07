package io.github.stomarver.aawt.client;

import io.github.stomarver.aawt.Aawt;
import eu.midnightdust.lib.config.EntryInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The item drawn in front of a config entry's label.
 *
 * <p>Keys are the field names of {@link io.github.stomarver.aawt.AawtConfig} - the same names the
 * config file uses - so an entry and its icon cannot drift apart silently: rename a field and the
 * icon simply stops matching, it does not end up on the wrong row.
 *
 * <p>Add a row to {@link #ICONS} to give another entry an icon, or drop the row to go back to a
 * plain label. Nothing else has to change.
 */
public final class ConfigEntryIcons {
	/** Owner of the head shown next to "Severed Player Head Naming": jeb_, of the rainbow sheep. */
	private static final String HEAD_PROFILE = "jeb_";

	/** Owner of the head shown next to "Severed Player Head Description": kingbdogz. */
	private static final String LORE_HEAD_PROFILE = "kingbdogz";

	private static final Map<String, Supplier<ItemStack>> ICONS = Map.of(
			"knockbackSuppression", () -> icon(Items.NETHERITE_AXE, nothing()),
			"beheading", () -> icon(Items.ENCHANTED_BOOK, glint()),
			"cleaving", () -> icon(Items.ENCHANTED_BOOK, glint()),
			"playerHeadNaming", () -> icon(Items.PLAYER_HEAD, head(HEAD_PROFILE)),
			"playerHeadLore", () -> icon(Items.PLAYER_HEAD, head(LORE_HEAD_PROFILE)));

	/**
	 * Built on first use and kept: the stacks never change, and drawing happens every frame.
	 *
	 * <p>A build that fails is <i>not</i> remembered - the row is simply left without an icon for
	 * that frame and the next draw tries again, so a screen opened too early recovers on its own
	 * instead of losing its icons for the whole session.
	 */
	private static final Map<String, ItemStack> built = new HashMap<>();

	/** Fields whose build already failed once, so that the warning is not repeated every frame. */
	private static final Set<String> reported = new HashSet<>();

	private ConfigEntryIcons() {
	}

	/**
	 * Icon of an entry, or {@code null} when the row is meant to stay a plain label - because no icon
	 * was asked for, or because building one failed. A failure is logged once and stays local to that
	 * row: the other icons keep working and the row is retried on the next draw.
	 */
	@Nullable
	public static ItemStack forEntry(@Nullable EntryInfo info) {
		if (info == null) {
			return null;
		}

		Supplier<ItemStack> icon = ICONS.get(info.fieldName);

		if (icon == null) {
			return null;
		}

		ItemStack cached = built.get(info.fieldName);

		if (cached != null) {
			return cached;
		}

		try {
			cached = icon.get();
		} catch (Throwable t) {
			if (reported.add(info.fieldName)) {
				Aawt.LOGGER.warn("Config entry icon for '{}' is unavailable, the row stays a plain label",
						info.fieldName, t);
			}

			return null;
		}

		built.put(info.fieldName, cached);

		return cached;
	}

	/**
	 * An icon stack that can be built at any moment, the main menu included.
	 *
	 * <p>Since 26.3 an item's data components are no longer baked during bootstrap: the registry
	 * bakes them when registry data is applied, which on the client happens while a world loads. At
	 * the main menu {@code Items.NETHERITE_AXE.builtInRegistryHolder().components()} is still
	 * unbound, so the plain {@code new ItemStack(item)} constructor - which reads exactly that -
	 * dies with "Components not bound yet". A config screen opened from Mod Menu is opened from the
	 * main menu, so the plain constructor is not an option here.
	 *
	 * <p>What is done instead is what MidnightLib itself does for its own entry icons: the stack is
	 * built around {@link Holder#direct(Object, DataComponentMap)}, a holder that carries its own
	 * components and therefore needs no registry baking. The one component the renderer cannot do
	 * without is {@link DataComponents#ITEM_MODEL} - with it missing the item model resolver draws
	 * nothing at all - and for a vanilla item it is simply the item's own id. Everything else comes
	 * from {@link DataComponents#COMMON_ITEM_COMPONENTS}, the same base {@code Item.Properties}
	 * starts from.
	 */
	private static ItemStack icon(Item item, Consumer<DataComponentMap.Builder> extras) {
		DataComponentMap.Builder components = DataComponentMap.builder()
				.addAll(DataComponents.COMMON_ITEM_COMPONENTS);
		components.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item));
		extras.accept(components);

		return new ItemStack(Holder.direct(item, components.build()));
	}

	private static Consumer<DataComponentMap.Builder> nothing() {
		return components -> {
		};
	}

	/** The enchanted-book glint, without putting a single enchantment on the icon. */
	private static Consumer<DataComponentMap.Builder> glint() {
		return components -> components.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
	}

	/**
	 * A player head carrying jeb_'s profile. The profile is left <i>unresolved</i> on purpose: the
	 * client then looks the skin up itself, exactly as it does for
	 * {@code /give @s player_head[profile="jeb_"]}, so the icon shows the real rainbow head. Where
	 * no lookup is possible - offline, or a profile service that refuses - the head is drawn with
	 * the default skin and the entry still works.
	 */
	private static Consumer<DataComponentMap.Builder> head(String profile) {
		return components -> components.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(profile));
	}
}
