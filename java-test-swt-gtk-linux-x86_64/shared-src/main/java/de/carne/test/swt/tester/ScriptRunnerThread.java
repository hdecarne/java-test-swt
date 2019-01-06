/*
 * Copyright (c) 2017-2019 Holger de Carne and contributors, All Rights Reserved.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import de.carne.boot.logging.Log;
import de.carne.boot.logging.LogLevel;
import de.carne.test.swt.platform.PlatformHelper;
import de.carne.util.Strings;

/**
 * {@linkplain Thread} class used to run the configured script actions against the SWT application.
 */
final class ScriptRunnerThread extends Thread {

	private static final Log LOG = new Log();

	private final Thread displayThread;
	private final Iterable<ScriptAction> actions;
	private final boolean ignoreRemaining;
	private final Duration timeout;
	private final AtomicReference<AssertionError> assertionStatus = new AtomicReference<>();

	ScriptRunnerThread(Iterable<ScriptAction> actions, boolean ignoreRemaining, Duration timeout) {
		super(ScriptRunnerThread.class.getName());
		this.displayThread = Thread.currentThread();
		this.actions = actions;
		this.ignoreRemaining = ignoreRemaining;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		LOG.info("{0} started", Thread.currentThread().getName());

		try {
			waitTrigger(this::displayAvailableTrigger);

			LOG.debug("Display available on display thread");

			waitTrigger(this::initialShellVisibleTrigger);

			LOG.debug("Initial Shell is visible; starting actions...");

			Display display = getDisplay();
			List<String> remainingShellTexts;

			try {
				Assertions.assertTimeoutPreemptively(this.timeout, () -> runActions(display));

				LOG.debug("All actions processed; cleaning up...");
			} finally {
				remainingShellTexts = disposeRemaining(display);
			}
			if (!this.ignoreRemaining && !remainingShellTexts.isEmpty()) {
				Assertions.fail("Remaining Shells detected: " + Strings.join(remainingShellTexts, ", "));
			}
		} catch (Exception e) {
			this.assertionStatus.set(new AssertionFailedError("Uncaught exception: " + e.getClass().getName(), e));
		} catch (AssertionError e) {
			this.assertionStatus.set(e);
		}
	}

	private void runActions(Display display) throws InterruptedException {
		int actionId = 1;

		for (ScriptAction action : this.actions) {
			runAction(display, action, actionId);
			actionId++;
		}

	}

	private void runAction(Display display, ScriptAction action, int actionId) throws InterruptedException {
		if (action.isAsync()) {
			runNoWait(display, action);

			Timing.step();

			LOG.debug("Action #{0} triggered", actionId);
		} else {
			long start = System.nanoTime();

			runWait(display, action);

			long elapsed = System.nanoTime() - start;

			Timing.step();

			LOG.debug("Action #{0} executed (took {1} ms)", actionId, elapsed / 1000000);
		}
	}

	private List<String> disposeRemaining(Display display) {
		List<String> remainingShellTexts = new ArrayList<>();

		if (PlatformHelper.closeNativeDialogs()) {
			LOG.log((this.ignoreRemaining ? LogLevel.LEVEL_INFO : LogLevel.LEVEL_WARNING), null,
					"Closing native dialog");

			remainingShellTexts.add("<native dialog>");
		}
		if (!display.isDisposed()) {
			runWait(display, () -> disposeRemaining0(remainingShellTexts, display));
		}
		return remainingShellTexts;
	}

	private List<String> disposeRemaining0(List<String> remainingShellTexts, Display display) {
		if (!display.isDisposed()) {
			Shell[] shells = display.getShells();

			for (Shell shell : shells) {
				if (!shell.isDisposed()) {
					String shellText = shell.getText();

					LOG.log((this.ignoreRemaining ? LogLevel.LEVEL_INFO : LogLevel.LEVEL_WARNING), null,
							"Closing remaining Shell ''{0}''", shellText);

					remainingShellTexts.add(shellText);
					shell.close();
					shell.dispose();
				}
			}
			display.dispose();
		}
		return remainingShellTexts;
	}

	public Optional<AssertionError> assertionStatus() {
		return Optional.ofNullable(this.assertionStatus.get());
	}

	private Display getDisplay() {
		Display display = Display.findDisplay(this.displayThread);

		Assertions.assertNotNull(display, "No Display found");

		return Objects.requireNonNull(display);
	}

	private void waitTrigger(BooleanSupplier trigger) throws InterruptedException {
		Timing wait = new Timing();

		while (!trigger.getAsBoolean()) {
			wait.step("Timeout execeeded while waiting for trigger");
		}
	}

	private boolean displayAvailableTrigger() {
		return Display.findDisplay(this.displayThread) != null;
	}

	private boolean initialShellVisibleTrigger() {
		Display display = Display.findDisplay(this.displayThread);

		return display != null && runWait(display, () -> {
			Shell[] shells = (!display.isDisposed() ? display.getShells() : new Shell[0]);

			return shells.length > 0 && shells[0].isVisible();
		}).booleanValue();
	}

	private void runNoWait(Display display, Runnable runnable) {
		display.asyncExec(runnable);
	}

	private void runWait(Display display, Runnable runnable) {
		if (Thread.currentThread().equals(display.getThread())) {
			runnable.run();
		} else {
			checkNativeDialog();
			try {
				display.syncExec(runnable);
			} catch (RuntimeException e) {
				Throwable cause = e.getCause();

				if (cause instanceof AssertionError) {
					throw (AssertionError) cause;
				}
				throw e;
			}
		}
	}

	private <T> T runWait(Display display, Supplier<T> supplier) {
		final AtomicReference<T> resultHolder = new AtomicReference<>();

		if (Thread.currentThread().equals(display.getThread())) {
			resultHolder.set(supplier.get());
		} else {
			checkNativeDialog();
			try {
				display.syncExec(() -> resultHolder.set(supplier.get()));
			} catch (RuntimeException e) {
				Throwable cause = e.getCause();

				if (cause instanceof AssertionError) {
					throw (AssertionError) cause;
				}
				throw e;
			}
		}
		return resultHolder.get();
	}

	private void checkNativeDialog() {
		if (PlatformHelper.inNativeDialog()) {
			Assertions.fail("Native dialog detected");
		}
	}

}
