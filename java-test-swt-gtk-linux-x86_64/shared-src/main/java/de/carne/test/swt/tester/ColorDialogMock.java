/*
 * Copyright (c) 2017-2020 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.test.swt.tester;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;

import de.carne.boot.logging.Log;
import mockit.Mock;
import mockit.MockUp;

/**
 * Mock for {@linkplain ColorDialog} dialog.
 */
public final class ColorDialogMock {

	private static final Log LOG = new Log();

	private @Nullable RGB nextResult = null;

	@SuppressWarnings("unused")
	private final MockUp<ColorDialog> mockUp = new MockUp<ColorDialog>() {

		@Mock
		public @Nullable RGB open() {
			return mockOpen();
		}

	};

	/**
	 * Sets the result for the next call to {@linkplain ColorDialog#open()}.
	 *
	 * @param result the result for the next call to {@linkplain ColorDialog#open()}.
	 */
	public void result(RGB result) {
		this.nextResult = result;
	}

	@Nullable
	RGB mockOpen() {
		RGB result = this.nextResult;

		LOG.info("ColorDialog.open() = {0}", result);

		this.nextResult = null;
		return result;
	}

}
