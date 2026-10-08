package io.github.stomarver.aawt;

import eu.midnightdust.lib.config.MidnightConfig;

/**
 * Config entries of the mod. The file is {@code config/aawt.json} on both sides and is
 * editable in-game with Mod Menu or with {@code /midnightconfig aawt}.
 *
 * <p>MidnightLib is JiJ'd by this mod, so no separate dependency is needed. Entry names must not be
 * changed once released: they are the JSON keys of the saved file.
 */
public class AawtConfig extends MidnightConfig {

	/**
	 * Knockback suppression of an axe, enchantment independent. {@code 0.0} disables it, {@code 1.0}
	 * removes the knockback completely. 0.4 equals a full set of netherite armor (4 * 10%).
	 */
	@Entry(min = 0.0, max = 1.0, isSlider = true)
	public static double knockbackSuppression = 0.4;

	/**
	 * Whether the Beheading enchantment exists. Registry level: the data file of the enchantment is
	 * only added to the loaded resource packs while this is {@code true}, so turning it off removes
	 * the enchantment from the registry on the next {@code /reload} or restart - from the enchanting
	 * table, from loot, from trades, from anvils, from {@code /enchant} and from mod browsers alike.
	 */
	@Entry
	public static boolean beheading = true;

	/** Whether the Cleaving enchantment exists. See {@link #beheading}. */
	@Entry
	public static boolean cleaving = true;

	/**
	 * Display name of a dropped player head. Placeholders: {@code %username%} - the name of the
	 * player or mannequin that was beheaded, {@code %killer%} - the name of the killer,
	 * {@code %uuid%} - the UUID of the victim. Curly braces ({@code {username}}) work as well. A
	 * translation key inside the template - like {@code aawt.head_naming.head_of} - becomes text in
	 * the language of whoever reads the name; the default template uses exactly that key. An empty
	 * string leaves the vanilla name of the head.
	 */
	@Entry
	public static String playerHeadNaming = "aawt.head_naming.head_of %username%";

	/**
	 * Template of the description line of a dropped player head: the grey line a hover draws right
	 * under the item name, in the slot where vanilla draws its "Dynamic" profile line. Placeholders
	 * are the ones of {@link #playerHeadNaming}; a translation key inside the template - like
	 * {@code aawt.head_description.chopped_by} - becomes text in the language of whoever hovers the head,
	 * so the default reads "Отсечена игроком X" in Russian and "Chopped by X" in English. Nothing is
	 * ever inserted between the parts: no colon, no separator, the spacing is the template's own. A
	 * template without any key is drawn as it is. An empty string silences the slot altogether: no
	 * description and no vanilla "Dynamic" line either. The key {@code aawt.head_description.dynamic}
	 * - worded exactly like vanilla's {@code component.profile.dynamic} - brings that line back as
	 * this mod's own grey line.
	 */
	@Entry
	public static String playerHeadLore = "aawt.head_description.chopped_by %killer%";

	/** Registers the config and reads {@code config/aawt.json}. Called once from both sides. */
	public static void init() {
		MidnightConfig.init(Aawt.MOD_ID, AawtConfig.class);
	}
}
