# Axes Are Weapons Too

**Axes Are Weapons Too** (aawt) is a server-side Fabric mod for Minecraft
26.3. It reduces the knockback from axe hits, adds two enchantments for axes,
stops the anvil from mixing weapon and tool enchantments on one axe, and names
the player heads that Beheading drops.

- **Minecraft:** 26.3 (Java Edition)
- **Loader:** Fabric 0.19.5 and later
- **Java:** 25
- **Side:** server-side, client optional
- **Source:** <https://github.com/stomarver/Axes-Are-Weapons-Too>

Chances, levels, groups and head templates are described in [wiki.md](wiki.md).

## Features

**Knockback suppression.** The server multiplies the knockback of every axe
hit by `1 - knockbackSuppression`. The default value 0.4 equals the knockback
resistance of a full set of netherite armor, so an axe hit pushes its target
as little as a normal hit would push a player in full netherite. Boats and
minecarts are not affected.

**Beheading and Cleaving.** Beheading gives a chance to drop the victim's head
on a kill, player heads included. Cleaving makes axe hits ignore part of a
shield's protection and keep the shield disabled longer. Both are defined in a
built-in data pack and both can be turned off completely.

**Weapon and tool groups.** Sharpness, Smite, Bane of Arthropods and Cleaving
are weapon enchantments; Efficiency, Fortune and Silk Touch are tool
enchantments. The anvil refuses to combine the two groups on one axe, so a
combat axe and a work axe cannot be merged into one item. Beheading combines
with both groups.

**Severed player heads.** A head dropped by Beheading is named from a
template; the default reads "Head of st0m4rv3r". Translation keys inside the
template are rendered by each client in its own language. Under the name the
head shows a grey description line, by default "Chopped by X", in place of the
vanilla "Dynamic" line. An empty template value removes the line.

## Installation

Put the jar in the server's `mods/` folder. Clients do not need the mod: a
client without it sees severed heads with their vanilla profile name and the
usual "Dynamic" line, and no suppression line in axe tooltips. In singleplayer
the mod goes in the client's `mods/` folder and the integrated server loads it
from there. If the server does not have the mod, the client mod does nothing:
all mechanics run on the server.

## Compatibility

| Mod | Type | Role |
| --- | --- | --- |
| Fabric API | required | lifecycle events and the settings payload |
| MidnightLib | bundled | config file and screen, jar-in-jar, never installed separately |
| Mod Menu | optional | the entry to the config screen |
| Enchantment Descriptions | optional | enchantment descriptions in tooltips, from the `.desc` keys |
| Axes Are Weapons | incompatible | merges the groups this mod separates; the loader refuses to load both |

## Configuration

Settings are stored in `config/aawt.json` and can be edited by hand or through
the config screen (Mod Menu > Axes Are Weapons Too). The mechanics read the
server's file. A client on a foreign server receives the server's values in a
payload and uses its own file for display only.

| Key | Default | Purpose |
| --- | --- | --- |
| `knockbackSuppression` | `0.4` | how much of the knockback from axe hits is removed |
| `beheading` | `true` | registers Beheading |
| `cleaving` | `true` | registers Cleaving |
| `playerHeadNaming` | `aawt.head_naming.head_of %username%` | head name template |
| `playerHeadLore` | `aawt.head_description.chopped_by %killer%` | description line template |
