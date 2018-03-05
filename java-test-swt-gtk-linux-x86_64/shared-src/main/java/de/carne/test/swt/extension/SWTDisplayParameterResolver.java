/*
 * Copyright (c) 2017-2018 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.test.swt.extension;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import de.carne.check.Check;
import de.carne.check.Nullable;

/**
 * {@linkplain ParameterResolver} that takes care of creation and disposal of a needed SWT {@linkplain Display} during
 * test execution.
 */
public class SWTDisplayParameterResolver implements ParameterResolver, AfterAllCallback {

	private static final Namespace EXTENSION_NAMESPACE = Namespace.create(SWTDisplayParameterResolver.class);
	private static final String DISPLAY_KEY = "Display";

	@Override
	public boolean supportsParameter(@Nullable ParameterContext parameterContext,
			@Nullable ExtensionContext extensionContext) {
		ParameterContext checkedParameterContext = Check.notNull(parameterContext);

		return checkedParameterContext.getParameter().getType().equals(Display.class);
	}

	@Override
	public Object resolveParameter(@Nullable ParameterContext parameterContext,
			@Nullable ExtensionContext extensionContext) {
		ExtensionContext checkedExtensionContext = Check.notNull(extensionContext);
		Store store = checkedExtensionContext.getParent().get().getStore(EXTENSION_NAMESPACE);
		Object displayObject = store.get(DISPLAY_KEY);

		if (displayObject == null) {
			displayObject = new Display();
			store.put(DISPLAY_KEY, displayObject);
		}
		return Check.isInstanceOf(displayObject, Display.class);
	}

	@Override
	public void afterAll(@Nullable ExtensionContext context) {
		ExtensionContext checkedExtensionContext = Check.notNull(context);
		Store store = checkedExtensionContext.getStore(EXTENSION_NAMESPACE);
		Object displayObject = store.get(DISPLAY_KEY);

		if (displayObject != null) {
			Check.isInstanceOf(displayObject, Display.class).dispose();
		}
	}

}
