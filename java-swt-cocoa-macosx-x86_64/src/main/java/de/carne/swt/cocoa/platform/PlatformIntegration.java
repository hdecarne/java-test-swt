/*
 * Copyright (c) 2007-2018 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.swt.cocoa.platform;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;

import de.carne.swt.events.EventConsumer;
import de.carne.swt.events.EventReceiver;

/**
 * Cocoa platform integration.
 */
@SuppressWarnings("squid:S2176")
public class PlatformIntegration extends de.carne.swt.platform.PlatformIntegration {

	@Override
	protected int internalGetButtonOrder() {
		return SWT.RIGHT_TO_LEFT;
	}

	@Override
	protected boolean internalIsCocoa() {
		return true;
	}

	@Override
	protected void internalCocoaAddAboutSelectionAction(Display display, Consumer<SelectionEvent> action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_ABOUT, EventConsumer.selected(action));
	}

	@Override
	protected void internalCocoaAddAboutSelectionAction(Display display, Runnable action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_ABOUT, EventReceiver.any(action));
	}

	@Override
	protected void internalCocoaAddPreferencesSelectionAction(Display display, Consumer<SelectionEvent> action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_PREFERENCES, EventConsumer.selected(action));
	}

	@Override
	protected void internalCocoaAddPreferencesSelectionAction(Display display, Runnable action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_PREFERENCES, EventReceiver.any(action));
	}

	@Override
	protected void internalCocoaAddQuitSelectionAction(Display display, Consumer<SelectionEvent> action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_QUIT, EventConsumer.selected(action));
	}

	@Override
	protected void internalCocoaAddQuitSelectionAction(Display display, Runnable action) {
		addSystemMenuItemSelectionListener(display, SWT.ID_QUIT, EventReceiver.any(action));
	}

	private void addSystemMenuItemSelectionListener(Display display, int itemId, Listener listener) {
		for (MenuItem menuItem : display.getSystemMenu().getItems()) {
			if (menuItem.getID() == itemId) {
				menuItem.addListener(SWT.Selection, listener);
				menuItem.addListener(SWT.DefaultSelection, listener);
				break;
			}
		}
	}

}
