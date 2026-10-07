package io.github.stomarver.aawt.pack;

import io.github.stomarver.aawt.Aawt;
import io.github.stomarver.aawt.AawtConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * The resource pack that decides whether {@code aawt:beheading} and {@code aawt:cleaving}
 * exist at all.
 *
 * <p>The two enchantment definitions live in {@code aawt_data/data/aawt/enchantment/}
 * inside the mod jar - deliberately <b>not</b> under {@code data/}, so nothing loads them on its
 * own. This pack serves them into the loaded packs while the matching config entry is {@code true}
 * and hides them while it is {@code false}. Because the registry is built from the loaded packs, the
 * config entry therefore works on the registry: an enchantment that is turned off is not registered,
 * so it is gone from the enchanting table, from loot, from trades, from anvils, from
 * {@code /enchant} and from every mod that browses the registry. {@code /reload} or a restart picks
 * the change up.
 *
 * <p>Tags that point at the two ids carry {@code "required": false}, so a missing enchantment does
 * not break a tag.
 *
 * <p>The pack is injected by {@link io.github.stomarver.aawt.mixin.PackRepositoryMixin} at the
 * <i>front</i> of the selected packs, which is the lowest priority: any real data pack can still
 * override or replace the definitions.
 */
public final class ConditionalEnchantmentPack extends PathPackResources {
	private static final Logger LOGGER = LoggerFactory.getLogger(Aawt.MOD_ID);

	/** Folder inside the mod jar that acts as the pack root. */
	private static final String ROOT_FOLDER = "aawt_data";

	/** Directory of the pack that this class filters, relative to the namespace. */
	private static final String ENCHANTMENT_DIRECTORY = "enchantment";

	private static final PackLocationInfo LOCATION = new PackLocationInfo(
		Aawt.MOD_ID + "_conditional_enchantments",
		Component.literal("Axes Are Weapons Too enchantments"),
		PackSource.BUILT_IN,
		Optional.empty());

	private static PackResources instance;
	private static boolean searched;

	private ConditionalEnchantmentPack(Path root) {
		super(LOCATION, root);
	}

	/**
	 * The pack, or nothing if the mod jar does not contain {@value #ROOT_FOLDER} (which would mean
	 * the resources were not packaged).
	 */
	public static synchronized Optional<PackResources> get() {
		if (!searched) {
			searched = true;

			Optional<Path> root = FabricLoader.getInstance().getModContainer(Aawt.MOD_ID)
				.flatMap(mod -> mod.findPath(ROOT_FOLDER));

			if (root.isPresent()) {
				instance = new ConditionalEnchantmentPack(root.get());
			} else {
				LOGGER.warn("No {} folder in the mod jar, the enchantments cannot be registered", ROOT_FOLDER);
			}
		}

		return Optional.ofNullable(instance);
	}

	@Override
	public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
		return isVisible(type, id) ? super.getResource(type, id) : null;
	}

	@Override
	public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
		if (type != PackType.SERVER_DATA || !Aawt.MOD_ID.equals(namespace)) {
			return;
		}

		super.listResources(type, namespace, directory, (id, supplier) -> {
			if (isVisible(type, id)) {
				output.accept(id, supplier);
			}
		});
	}

	/**
	 * Only {@code aawt} data exists here. The overridden version also stops the inherited one
	 * from listing {@code assets/}, which this pack does not have.
	 */
	@Override
	public Set<String> getNamespaces(PackType type) {
		return type == PackType.SERVER_DATA ? Set.of(Aawt.MOD_ID) : Set.of();
	}

	/**
	 * Reported as supporting the data pack format of the running game, so no {@code pack.mcmeta}
	 * with a hardcoded number has to be shipped.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMetadataSection(MetadataSectionType<T> type) {
		if (type == PackMetadataSection.SERVER_TYPE || type == PackMetadataSection.FALLBACK_TYPE) {
			PackFormat format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);

			return (T) new PackMetadataSection(LOCATION.title(), new InclusiveRange<>(format));
		}

		return null;
	}

	/** Whether the file behind this id should be handed out right now. */
	private static boolean isVisible(PackType type, Identifier id) {
		if (type != PackType.SERVER_DATA || !Aawt.MOD_ID.equals(id.getNamespace())) {
			return false;
		}

		String path = id.getPath();

		if (!path.startsWith(ENCHANTMENT_DIRECTORY + "/")) {
			return false;
		}

		return switch (path.substring(ENCHANTMENT_DIRECTORY.length() + 1)) {
			case "beheading.json" -> AawtConfig.beheading;
			case "cleaving.json" -> AawtConfig.cleaving;
			default -> true;
		};
	}
}
