# Axes Are Weapons Too

Installation, compatibility and configuration — in [README.md](README.md).

Contents:

- [Gameplay](#gameplay)
  - [Knockback suppression](#knockback-suppression)
  - [Beheading and Cleaving](#beheading-and-cleaving)
  - [Weapon and tool groups](#weapon-and-tool-groups)
  - [Severed player heads](#severed-player-heads)
- [Technical details](#technical-details)

## Gameplay

### Knockback suppression

Every hit with any axe multiplies the knockback the target receives by one
minus the server's `knockbackSuppression` value. The default 0.4 removes 40% of
it — as much as a full netherite armor set. The multiplier applies after the
Knockback enchantment has contributed, so an axe enchanted with Knockback
suppresses its own enchantment along with everything else.

### Beheading and Cleaving

Both enchantments are plain data: a built-in pack serves their definitions
while the matching config entry is true and hides them otherwise, so a
switched-off enchantment is absent from the registry altogether. The
enchanting table and generated loot offer them at vanilla frequency — the
weights, 10 for Cleaving and 1 for Beheading, match their vanilla neighbours.

| Enchantment | Levels | Effect |
| --- | --- | --- |
| Beheading | I–III | drops the victim's head: 30/50/65% by level; a player head keeps the profile |
| Cleaving | I–V | ignores 20% of the shield's protection per level; +0.2 s shield cooldown per level |

Mob heads are the same ones a charged creeper's explosion leaves behind.

### Weapon and tool groups

Vanilla lets an axe carry both families; the mod draws the line at the anvil,
which refuses to combine a weapon-group enchantment with a tool-group one on
the same axe. The two vanilla tag families intersect exactly on the axes, so
no other item is affected. Beheading belongs to neither family and combines
with both.

| Group | Enchantments |
| --- | --- |
| Weapon | Sharpness, Smite, Bane of Arthropods, Cleaving |
| Tool | Efficiency, Fortune, Silk Touch |

### Severed player heads

The server builds a severed player head's name from the `playerHeadNaming`
template and stores it in the item name component. The default,
`aawt.head_naming.head_of %username%`, reads "Head of st0m4rv3r"; the prefix is
a translation key that stays a key inside the component, so each client
renders it in its own language.

| Placeholder | Value |
| --- | --- |
| `%username%` | the victim's name |
| `%killer%` | the attacker's name |
| `%uuid%` | the victim's UUID |
| translation key | text in the language of whoever reads the name |

A template whose placeholder has nothing to substitute — a mannequin without a
name — leaves the head its vanilla name rather than a dangling "Head of".

The `playerHeadLore` template is a grey non-italic line under the name. It
occupies the slot of the vanilla "Dynamic" line and replaces it: the default
`aawt.head_description.chopped_by %killer%` reads "Chopped by X".

| `playerHeadLore` value | Head dropped by Beheading |
| --- | --- |
| template | the description in the "Dynamic" slot |
| empty | neither description nor "Dynamic" |
| `aawt.head_description.dynamic` | the mod's line, worded as vanilla |

The vanilla line is gone from severed heads either way; the built-in key
`aawt.head_description.dynamic` is the only way to bring it back, styled as the
mod. Heads that Beheading did not drop carry no marker and keep their vanilla
tooltip, "Dynamic" included.

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
- the head name travels as an item name component with translatable parts; the
  description line travels as custom data under `aawt:lore_line` and is drawn
  by a client mixin in the "Dynamic" slot;
- the enchantment definitions live in a built-in pack the mod injects last, so
  any data pack can retune or replace them; tags reference the optional ids
  with `required: false`;
- no mappings: vanilla classes and Fabric API only.
