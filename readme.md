# Axes Are Weapons Too

**Axes Are Weapons Too** (aawt) is a server-side Fabric mod for Minecraft 26.3
that makes the axe a weapon of its own: axe hits suppress knockback, the
enchantments an axe carries split into a weapon and a tool family that the
anvil keeps apart, and Beheading takes the victim's head.

- **Minecraft:** 26.3 (Java Edition)
- **Loader:** Fabric 0.19.5 and later
- **Java:** 25
- **Side:** server-side, client optional

Chances, levels, groups and head templates in full — in [Wiki](wiki.md).

## Features

**Knockback suppression.** Every hit with any axe removes a configured share
of the knockback the target receives; the default 40% is what a full netherite
armor set removes. The share applies after the Knockback enchantment has
contributed, so the enchantment is suppressed along with everything else.

**Beheading and Cleaving.** Beheading makes a kill with the axe drop the
victim's head, player heads included; Cleaving punches through a shield's
protection and keeps the shield disabled longer. Both are data-driven and
switchable off at the registry level.

**Weapon and tool groups.** The enchantments an axe carries form two families
the anvil refuses to mix on one axe, so a battle axe and a chopping axe stay
separate items.

**Severed player heads.** A head dropped by Beheading is named from a template
— a translation key inside it renders in the reader's own language — and
carries a grey description line in the slot of the vanilla "Dynamic" line. An
empty template value silences the line altogether.

## Installation

A jar in the server's `mods/` folder enables every mechanic; clients are not
required. Without the mod a client sees severed heads under their vanilla
profile name and "Dynamic" line, and no axe line in tooltips. In singleplayer
the mod goes on the client, whose integrated server loads it. On a server
without the mod the client mod stays inert: the server is authoritative.

## Compatibility

| Mod | Type | Role |
| --- | --- | --- |
| Fabric API | required | lifecycle events and the settings payload |
| MidnightLib | bundled | config file and screen; jar-in-jar, never installed on its own |
| Mod Menu | optional | the mod page and the entry to the config screen |
| Enchantment Descriptions | optional | enchantment descriptions in tooltips, from the `.desc` keys |
| Axes Are Weapons | incompatible | merges the families this mod splits; loader refuses to load both |

## Configuration

Settings live in `config/aawt.json`, editable by hand or through the screen
(Mod Menu — Axes Are Weapons Too). Mechanics read the server file only; on a
foreign server the client receives the values in a payload and uses its own
file for display alone.

| Key | Default | Purpose |
| --- | --- | --- |
| `knockbackSuppression` | `0.4` | share of knockback an axe hit suppresses |
| `beheading` | `true` | registers Beheading |
| `cleaving` | `true` | registers Cleaving |
| `playerHeadNaming` | `aawt.head_naming.head_of %username%` | head name template |
| `playerHeadLore` | `aawt.head_description.chopped_by %killer%` | description line template |
