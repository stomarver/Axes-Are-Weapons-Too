# Axes Are Weapons Too

Contents:

- [Gameplay](#gameplay)
  - [Knockback suppression](#knockback-suppression)
  - [Beheading and Cleaving](#beheading-and-cleaving)
  - [Weapon and tool groups](#weapon-and-tool-groups)
  - [Severed player heads](#severed-player-heads)
- [Technical details](#technical-details)

## Gameplay

### Knockback suppression

The server multiplies the knockback of every hit dealt with an axe by
`1 - knockbackSuppression` before the target is pushed. The reduction covers
the base push of the hit and the sprint bonus, and it stacks with the target's
own knockback resistance. Boats and minecarts are not affected.

The default value 0.4 equals the knockback resistance of a full set of
netherite armor: an axe hit pushes its target as little as a normal hit would
push a player in full netherite.

### Beheading and Cleaving

Both enchantments are defined in a built-in data pack. If the matching config
entry is off, the enchantment is removed from the registry: it disappears
from the enchanting table, loot, trades, the anvil and `/enchant`. The weights
are matched to vanilla, so the table and generated loot roll them as often as
vanilla rolls enchants of the same weight: Cleaving has weight 10 like
Efficiency, Beheading has weight 1 like Silk Touch.

| Enchantment | Levels | Effect |
| --- | --- | --- |
| Beheading | I–III | chance to drop the victim's head on a kill: 30/50/65% by level; a player head keeps the victim's profile |
| Cleaving | I–V | ignores 20% of the shield's protection per level; the shield stays disabled 0.2 s longer per level |

Mob heads are the same ones a charged creeper's explosion drops.

### Weapon and tool groups

In vanilla an axe accepts enchantments of both families. The mod separates
them at the anvil: it refuses to combine a weapon-group enchantment with a
tool-group enchantment on one axe. Only axes accept both families, so no other
item is affected. Beheading belongs to neither group and combines with both.

| Group | Enchantments |
| --- | --- |
| Weapon | Sharpness, Smite, Bane of Arthropods, Cleaving |
| Tool | Efficiency, Fortune, Silk Touch |

### Severed player heads

The server names a severed player head from the `playerHeadNaming` template
and stores the name in the item name component. The default template
`aawt.head_naming.head_of %username%` reads "Head of st0m4rv3r". The prefix is
a translation key and stays a key inside the component, so each client renders
it in its own language.

| Placeholder | Value |
| --- | --- |
| `%username%` | the victim's name |
| `%killer%` | the attacker's name |
| `%uuid%` | the victim's UUID |
| translation key | text in the language of whoever reads the name |

If a placeholder has nothing to substitute, for example on a mannequin without
a name, the head keeps its vanilla name instead of ending in a bare "Head of".

The `playerHeadLore` template adds a grey non-italic line under the name. It
takes the place of the vanilla "Dynamic" line, which is removed. The default
template `aawt.head_description.chopped_by %killer%` reads "Chopped by X".

| `playerHeadLore` value | Head dropped by Beheading |
| --- | --- |
| template | the description in the "Dynamic" slot |
| empty | neither description nor "Dynamic" |
| `aawt.head_description.dynamic` | the mod's grey line with the vanilla word "Dynamic" |

The key `aawt.head_description.dynamic` is the only way to bring the vanilla
word "Dynamic" back on severed heads; it is drawn in the mod's grey style.
Heads that Beheading did not drop keep their vanilla tooltip, "Dynamic"
included.

| Key | Text |
| --- | --- |
| `aawt.head_naming.head_of` | Head of |
| `aawt.head_description.killed_by` | Killed by |
| `aawt.head_description.chopped_by` | Chopped by |
| `aawt.head_description.dynamic` | Dynamic |

## Technical details

- nine mixins prefixed `aawt$`, among them the knockback calls,
  `Enchantment.areCompatible`, `ItemStack.getTooltipLines`, head name
  resolution and the pack repository;
- the head name travels as an item name component with translatable parts;
  the description line travels as custom data under `aawt:lore_line` and is
  drawn by a client mixin in the slot of the "Dynamic" line;
- the enchantment definitions live in a built-in pack the mod injects last, so
  any data pack can override them; tags reference the optional ids with
  `required: false`;
- no mappings: vanilla classes and Fabric API only.
