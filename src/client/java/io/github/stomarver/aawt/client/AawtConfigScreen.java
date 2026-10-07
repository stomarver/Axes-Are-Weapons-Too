package io.github.stomarver.aawt.client;

import io.github.stomarver.aawt.Aawt;
import eu.midnightdust.lib.config.ButtonEntry;
import eu.midnightdust.lib.config.MidnightConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * MidnightConfig's own screen, with an item in front of every label.
 *
 * <p>Everything here uses MidnightLib's public API - {@code list}, {@code ButtonEntry.title},
 * {@code ButtonEntry.info} and the screen's own {@code init}/{@code updateList}/
 * {@code extractRenderState} - so no mixin into the library is needed and the screen keeps all of
 * MidnightLib's behaviour: tabs, reset buttons, sliders, the {@code /midnightconfig} command and the
 * reload-on-change handling.
 *
 * <p>Two steps, both idempotent, because {@code updateList} rebuilds the rows whenever a value
 * changes: {@link #makeRoomForIcons()} moves the labels of the rows that have an icon one icon width
 * to the right and shortens their wrapping width by the same amount, and
 * {@link #drawIcons(GuiGraphicsExtractor)} then paints the items into the space that freed up. Rows
 * without an icon - comments, and any entry {@link ConfigEntryIcons} does not know - are left exactly
 * as MidnightLib drew them.
 *
 * <p>Icons are cosmetics, so nothing they do is allowed to take the screen down. A row whose icon
 * cannot be built is logged once by {@link ConfigEntryIcons} and stays a plain label, and should
 * anything else in the icon path throw, the first failure is logged once here and turns the feature
 * off for the rest of the session - either way what is left is a working MidnightConfig screen.
 */
public class AawtConfigScreen extends MidnightConfigScreen {
	/** Icons are the size of an item in an inventory slot. */
	private static final int ICON_SIZE = 16;

	/** What a label gives up for its icon: the icon plus a small gap. */
	private static final int ICON_MARGIN = 20;

	/** Where MidnightLib puts a row's label, and so where the icon goes. */
	private static final int LABEL_X = 12;

	/** Gap MidnightLib keeps between a label and the widget on its right. */
	private static final int WIDGET_GAP = 16;

	/** Set by the first failure in the icon path; the screen then behaves like the plain one. */
	private boolean iconsOff = false;

	/**
	 * Entries whose label has already been moved right. An icon is drawn into a row only once the row
	 * has given up the space for it, so an icon that starts building after the last layout pass waits
	 * for the next one instead of landing on top of its own label.
	 */
	private final Set<String> roomMade = new HashSet<>();

	public AawtConfigScreen(Screen parent, String modid) {
		super(parent, modid);
	}

	@Override
	public void init() {
		super.init();
		this.makeRoomForIcons();
	}

	@Override
	public void updateList() {
		super.updateList();
		this.makeRoomForIcons();
	}

	/** Moves the labelled rows right, so the icon has somewhere to be drawn. */
	private void makeRoomForIcons() {
		if (this.iconsOff) {
			return;
		}

		try {
			for (ButtonEntry entry : this.list.children()) {
				MultiLineTextWidget title = entry.title;
				String field = entry.info == null ? null : entry.info.fieldName;

				if (title == null || field == null || ConfigEntryIcons.forEntry(entry.info) == null) {
					continue;
				}

				title.setX(LABEL_X + ICON_MARGIN);
				title.setMaxWidth(Math.max(ICON_SIZE, this.labelWidth(entry) - ICON_MARGIN));
				this.roomMade.add(field);
			}
		} catch (Throwable t) {
			this.disableIcons(t, "making room for");
		}
	}

	/**
	 * How wide a row's label may be, measured the way MidnightLib measures it: up to the widget on the
	 * left of the value column, or up to the screen edge for rows that carry no widget. Taking the
	 * smallest widget X rather than MidnightLib's "first" or "last" one keeps a long label out of every
	 * widget of the row, whichever of them sits leftmost.
	 */
	private int labelWidth(ButtonEntry entry) {
		int limit = this.minecraft.getWindow().getGuiScaledWidth() - 24;

		for (AbstractWidget widget : entry.buttons) {
			limit = Math.min(limit, widget.getX() - WIDGET_GAP);
		}

		return limit;
	}

	/**
	 * Draws the icons. Called after {@code super}, which is where the rows were laid out - so
	 * {@code entry.getY()} is this frame's position - and where MidnightLib drew the labels, the
	 * widgets and the title, so the items end up on top of nothing but empty background.
	 *
	 * <p>The list clips its rows with scissors of its own, and these items are drawn outside of that
	 * call, so the same scissors are set here: an icon then slides in and out together with its row
	 * instead of appearing whole over the header separator or the buttons. The visibility test is the
	 * list's own, and {@code entry.getY()} is safe to read for every row: the list repositions
	 * <i>all</i> of them whenever the scroll amount changes, not just the visible ones.
	 */
	private void drawIcons(GuiGraphicsExtractor context) {
		if (this.iconsOff) {
			return;
		}

		try {
			context.enableScissor(
					Mth.clamp(this.list.getX(), 0, context.guiWidth()),
					Mth.clamp(this.list.getY(), 0, context.guiHeight()),
					Mth.clamp(this.list.getRight(), 0, context.guiWidth()),
					Mth.clamp(this.list.getBottom(), 0, context.guiHeight()));

			try {
				int top = this.list.getY();
				int bottom = this.list.getBottom();

				for (ButtonEntry entry : this.list.children()) {
					String field = entry.info == null ? null : entry.info.fieldName;
					ItemStack icon = field != null && this.roomMade.contains(field)
							? ConfigEntryIcons.forEntry(entry.info)
							: null;

					if (icon == null) {
						continue;
					}

					int y = entry.getY();

					if (y + entry.getHeight() < top || y > bottom) {
						continue;
					}

					int x = entry.title != null ? entry.title.getX() - ICON_MARGIN : LABEL_X;
					context.fakeItem(icon, x, y + 2);
				}
			} finally {
				context.disableScissor();
			}
		} catch (Throwable t) {
			this.disableIcons(t, "drawing");
		}
	}

	/** One line in the log, and the screen goes on without icons. */
	private void disableIcons(Throwable t, String what) {
		this.iconsOff = true;
		Aawt.LOGGER.error("Config entry icons are off for this session, the screen stays the plain MidnightConfig one (failed while {} them)", what, t);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		this.drawIcons(context);
	}
}
