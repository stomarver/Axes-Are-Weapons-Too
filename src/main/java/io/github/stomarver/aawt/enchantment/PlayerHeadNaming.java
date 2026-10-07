package io.github.stomarver.aawt.enchantment;

import io.github.stomarver.aawt.AawtConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the two head templates of {@link AawtConfig} - {@code playerHeadNaming} and
 * {@code playerHeadLore} - into the item name and the description line of a dropped player head.
 *
 * <p>Placeholders, accepted both as {@code %placeholder%} and as {@code {placeholder}}:
 *
 * <table>
 *   <caption>Placeholders</caption>
 *   <tr><td>{@code %username%}</td><td>aliases {@code %player%}, {@code %name%}</td><td>name of the
 *       beheaded player or mannequin</td></tr>
 *   <tr><td>{@code %killer%}</td><td>alias {@code %attacker%}</td><td>name of whoever landed the
 *       killing blow</td></tr>
 *   <tr><td>{@code %uuid%}</td><td></td><td>UUID of the victim</td></tr>
 * </table>
 *
 * <p>Both templates accept translation keys next to their placeholders: a key becomes text in the
 * language of whoever reads the head, on a name as well as on the description line.
 *
 * <p>The name is written to {@link net.minecraft.core.component.DataComponents#ITEM_NAME}, the plain
 * non-italic item name, so the head reads like a normal item instead of like something renamed in an
 * anvil. An empty config entry, or a template that resolves to nothing - {@code %username%} for a
 * mannequin that was never given a skin - leaves the head unnamed, and vanilla then shows its own
 * {@code block.minecraft.player_head[.named]}.
 *
 * <p>The description travels as one string - the {@code playerHeadLore} template with its
 * placeholders filled in - inside {@link net.minecraft.core.component.DataComponents#CUSTOM_DATA};
 * the client that hovers the head turns any translation key left in it into text of its own
 * language and draws the result as the grey line under the item name, in place of vanilla's
 * "Dynamic" profile line, see {@link io.github.stomarver.aawt.mixin.PlayerHeadLoreTooltipMixin}.
 * So the name is baked on the server, which drops the head, while the description follows the
 * language of whoever reads it.
 */
public final class PlayerHeadNaming {
	private PlayerHeadNaming() {
	}

	/**
	 * Name of the head, or {@code null} when the config asks for no name or the template has nothing
	 * left to say.
	 *
	 * @param profile skin profile that goes onto the head; may be an empty one or {@code null}
	 * @param victim  the beheaded entity
	 * @param killer  whoever landed the killing blow
	 */
	public static Component name(ResolvableProfile profile, LivingEntity victim, LivingEntity killer) {
		String template = AawtConfig.playerHeadNaming;

		if (template == null || template.isBlank()) {
			return null;
		}

		Map<String, String> values = placeholders(username(profile, victim), killerName(killer), victim);
		MutableComponent name = parse(template, segment -> substitute(segment, values));

		// A placeholder that had nothing to give - %username% for a mannequin that was never named -
		// would leave a dangling "Head of ", so the head stays unnamed and vanilla names it. A
		// template that is nothing but spaces names nothing either; a template carrying a
		// translation key never counts as empty, because only a client can say what the key reads as.
		boolean keyed = KEY_TOKEN.matcher(template).find();

		return name == null || !keyed && name.getString().isBlank() ? null : name;
	}

	/** Tag key inside {@code minecraft:custom_data} that carries the description line. */
	public static final String LORE_LINE_TAG = "aawt:lore_line";

	/**
	 * The description line of a dropped head with every placeholder already filled in; an empty
	 * string means "no line at all". Translation keys inside the result are left untouched: which
	 * language they speak is decided later, by {@link #parseLoreLine}, on the client that hovers the
	 * head. Every beheaded head carries the result, even the empty one: the carried line is what
	 * tells the client to make room - or not - where vanilla would draw its "Dynamic" profile line.
	 *
	 * @param profile skin profile that goes onto the head; may be an empty one or {@code null}
	 * @param victim  the beheaded entity
	 * @param killer  whoever landed the killing blow
	 */
	public static String loreLine(ResolvableProfile profile, LivingEntity victim, LivingEntity killer) {
		String template = AawtConfig.playerHeadLore;

		if (template == null || template.isBlank()) {
			return "";
		}

		String text = substitute(template, placeholders(username(profile, victim), killerName(killer), victim));

		// A placeholder with nothing to give silences the line instead of leaving it half-filled.
		return text == null || text.isBlank() ? "" : text;
	}

	/** A dotted lang key candidate inside a head name or description line, e.g. {@code aawt.head_description.chopped_by}. */
	private static final Pattern KEY_TOKEN = Pattern.compile("[a-z_][a-z_\\d-]*(?:\\.[a-z_\\d-]+)+");

	/**
	 * The line a client draws under the head's name: grey, not italic, with every translation key of
	 * the stored line turned into text of THAT client's language. The template carries its own
	 * spacing - {@code aawt.head_description.chopped_by %killer%} - so no separator is ever added.
	 */
	public static Component parseLoreLine(String line) {
		return parse(line, segment -> segment)
				.withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/**
	 * Splits {@code text} on translation-key tokens: every token becomes a translatable component,
	 * resolved later in the language of whoever reads it, and everything between tokens stays a
	 * literal, handed to {@code segments} first. A {@code null} coming back from {@code segments} -
	 * a placeholder with nothing to give - turns the whole result into {@code null}.
	 */
	private static MutableComponent parse(String text, UnaryOperator<String> segments) {
		MutableComponent out = Component.empty();
		Matcher keys = KEY_TOKEN.matcher(text);
		int last = 0;

		while (keys.find()) {
			String segment = segments.apply(text.substring(last, keys.start()));

			if (segment == null) {
				return null;
			}

			out.append(Component.literal(segment)).append(Component.translatable(keys.group()));
			last = keys.end();
		}

		String tail = segments.apply(text.substring(last));

		return tail == null ? null : out.append(Component.literal(tail));
	}

	private static Map<String, String> placeholders(String username, String killerName, LivingEntity victim) {
		Map<String, String> values = new LinkedHashMap<>();

		values.put("username", orEmpty(username));
		values.put("player", orEmpty(username));
		values.put("name", orEmpty(username));
		values.put("killer", orEmpty(killerName));
		values.put("attacker", orEmpty(killerName));
		values.put("uuid", victim.getUUID().toString());

		return values;
	}

	/** Name of the beheaded player or mannequin: its skin profile first, then a name tag, then nothing. */
	private static String username(ResolvableProfile profile, LivingEntity victim) {
		if (profile != null && profile.name().isPresent()) {
			return profile.name().get();
		}

		Component customName = victim.getCustomName();

		if (customName != null) {
			return customName.getString();
		}

		if (victim instanceof ServerPlayer player) {
			return player.getGameProfile().name();
		}

		return null;
	}

	/** Name of whoever landed the killing blow: the account name of a player, otherwise whatever the mob is called. */
	public static String killerName(LivingEntity killer) {
		if (killer instanceof ServerPlayer player) {
			return player.getGameProfile().name();
		}

		Component customName = killer.getCustomName();

		return customName != null ? customName.getString() : killer.getName().getString();
	}

	/**
	 * Substitutes every placeholder the template actually uses. Returns {@code null} when one of them
	 * had no value, so the caller can leave the head unnamed instead of writing a half-filled name.
	 */
	private static String substitute(String template, Map<String, String> values) {
		String text = template;
		boolean missing = false;

		for (Map.Entry<String, String> entry : values.entrySet()) {
			String percent = '%' + entry.getKey() + '%';
			String braces = '{' + entry.getKey() + '}';

			if ((text.contains(percent) || text.contains(braces)) && entry.getValue().isEmpty()) {
				missing = true;
			}

			text = text.replace(percent, entry.getValue()).replace(braces, entry.getValue());
		}

		return missing ? null : text;
	}

	private static String orEmpty(String value) {
		return value == null ? "" : value;
	}
}
