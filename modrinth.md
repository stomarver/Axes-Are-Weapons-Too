Axes Are Weapons Too is a server-side Fabric mod for Minecraft 26.3. Clients
do not need it installed. The mod reduces the knockback from axe hits, adds
two enchantments for axes, stops the anvil from mixing combat and work
enchantments on one axe, and renames the player heads that Beheading drops.

## Knockback suppression

An axe hits hard, but a vanilla swing sends the target flying after every
hit. With the mod the server multiplies the knockback of each axe hit by
`1 - knockbackSuppression`. The default value 0.4 equals the knockback
resistance of a full set of netherite armor, so an axe hit pushes its target
as little as a normal hit would push a player in full netherite. The sprint
bonus is reduced the same way, and the reduction stacks with the target's own
knockback resistance. Boats and minecarts are not affected.

## Beheading and Cleaving

**Beheading** (I–III) gives a chance to drop the victim's head on a kill: 30,
50 and 65% by level. A player head keeps the victim's profile and skin, mob
heads are the same ones a charged creeper drops.

**Cleaving** (I–V) makes an axe hit that breaks a shield ignore 20% of the
shield's protection per level and keep the shield disabled 0.2 s longer per
level.

Both enchantments are defined in a built-in data pack. Their weights match
vanilla enchants of the same kind (Cleaving 10 like Efficiency, Beheading 1
like Silk Touch), so the enchanting table and generated loot roll them at
vanilla frequency. Turning one off in the config removes it from the registry
completely: it disappears from the table, loot, trades, the anvil and
`/enchant`.

## Weapon and tool enchantments

Sharpness, Smite, Bane of Arthropods and Cleaving are weapon enchantments,
Efficiency, Fortune and Silk Touch are tool enchantments. The anvil refuses to
combine the two groups on one axe, so a combat axe and a work axe cannot be
merged into one item. Beheading belongs to neither group and combines with
both. Axes are the only items affected, because only axes accept both groups
in the first place.

## Severed player heads

A head dropped by Beheading is named from a template; the default reads "Head
of st0m4rv3r". Under the name the head shows a grey description line, by
default "Chopped by Alex", in place of the vanilla "Dynamic" line. Templates
take the placeholders %username%, %killer% and %uuid% and translation keys,
which each client renders in its own language. An empty template value removes
the line.

## Server and client

All mechanics run on the server. A client without the mod plays normally and
sees severed heads with their vanilla name and "Dynamic" line. With the mod on
the client, axe tooltips show the suppression line, severed heads show their
name and description, and the config screen appears under Mod Menu.

## Installation and compatibility

Put the jar in the server's `mods/` folder; in singleplayer, in the client's.

- Fabric API is required.
- MidnightLib is bundled in the jar, there is no need to install it
  separately.
- Mod Menu is optional and opens the config screen.
- Enchantment Descriptions is optional and shows the enchantment
  descriptions.
- Axes Are Weapons is incompatible: it merges the enchantment groups this mod
  separates, and the loader refuses to load both.

The config has five entries in `config/aawt.json`; clients receive the
server's values automatically. Full details, tables and translation keys are
in the
[wiki](https://github.com/stomarver/Axes-Are-Weapons-Too/blob/main/wiki.md).
