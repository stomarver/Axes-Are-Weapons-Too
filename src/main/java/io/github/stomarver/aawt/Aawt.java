package io.github.stomarver.aawt;

import io.github.stomarver.aawt.enchantment.AxeEnchantmentGroups;
import io.github.stomarver.aawt.enchantment.AawtEnchantments;
import io.github.stomarver.aawt.sync.SettingsPayload;
import io.github.stomarver.aawt.sync.SettingsSync;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Aawt implements ModInitializer {
	public static final String MOD_ID = "aawt";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Every identifier this mod owns: the settings payload, the enchantments, the group tags. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Creates config/aawt.json on first launch, loads it afterwards and registers the
		// config screen. MidnightLib hands that screen to Mod Menu automatically.
		MidnightConfig.init(MOD_ID, AawtConfig.class);

		// Registers the settings payload and starts handing it to every client that joins: this is
		// what makes the server authoritative (see EffectiveSettings).
		SettingsSync.register();

		// The two enchantments are served by ConditionalEnchantmentPack, which PackRepositoryMixin
		// puts into the loaded packs while their config entry is on. The config therefore decides what
		// the enchantment registry holds, and no registration call is needed here.

		// Once the world is up and the data pack tags are bound, say what this server enforces.
		ServerLifecycleEvents.SERVER_STARTED.register(Aawt::logAuthority);
	}

	private static void logAuthority(MinecraftServer server) {
		SettingsPayload settings = EffectiveSettings.get();
		Registry<Enchantment> enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

		LOGGER.info("Server settings: knockback suppression {}%; enchantments - {}, {}; groups - weapon {}, tool {}; head naming '{}'; head lore '{}'",
				(int) Math.round(settings.knockbackSuppression() * 100.0),
				enchantmentState(AawtConfig.beheading, AawtEnchantments.BEHEADING, enchantments),
				enchantmentState(AawtConfig.cleaving, AawtEnchantments.CLEAVING, enchantments),
				groupContents(enchantments, AxeEnchantmentGroups.WEAPON),
				groupContents(enchantments, AxeEnchantmentGroups.TOOL),
				AawtConfig.playerHeadNaming,
				AawtConfig.playerHeadLore);
	}

	/**
	 * What the config asks for and what the registry actually holds: "beheading on (registered)".
	 * The two disagree only until the next {@code /reload} after a toggle was changed.
	 */
	private static String enchantmentState(boolean enabled, ResourceKey<Enchantment> enchantment, Registry<Enchantment> enchantments) {
		return "%s %s (%s)".formatted(enchantment.identifier().getPath(), enabled ? "on" : "off",
				enchantments.get(enchantment).isPresent() ? "registered" : "not registered");
	}

	/** Resolved contents of an enchantment group tag, so a data pack override is visible in the log. */
	private static String groupContents(Registry<Enchantment> enchantments, TagKey<Enchantment> group) {
		List<String> names = new ArrayList<>();

		for (Holder<Enchantment> holder : enchantments.getTagOrEmpty(group)) {
			names.add(holder.unwrapKey().map(key -> key.identifier().getPath()).orElse("?"));
		}

		Collections.sort(names);

		return names.toString();
	}
}
