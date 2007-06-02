/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;

import javax.management.NotCompliantMBeanException;

public class ObjectManagementMonitor extends AbstractTerracottaMBean implements
		ObjectManagementMonitorMBean {

	private static final TCLogger logger = TCLogging
			.getLogger(ObjectManagementMonitor.class);

	private volatile GCComptroller gcController;

	private final GCRunner gcRunner;

	public ObjectManagementMonitor() throws NotCompliantMBeanException {
		super(ObjectManagementMonitorMBean.class, false);

		gcRunner = new GCRunner() {
			private boolean isRunning = false;

			public void run() {
				setRunningState();
				gcController.startGC();
				setStopState();
			}

			private synchronized void setRunningState() {
				if (isRunning) {
					throw new UnsupportedOperationException(
							"Cannot run GC because GC is already running.");
				}
				isRunning = true;
				logger.info("Running GC.");
			}

			private synchronized void setStopState() {
				if (!isRunning) {
					throw new UnsupportedOperationException(
							"Cannot stop GC because GC is not running.");
				}
				isRunning = false;
				logger.info("GC finished.");
			}

			public synchronized boolean isGCRunning() {
				return isRunning;
			}
		};
	}

	public boolean isGCRunning() {
		return gcRunner.isGCRunning();
	}

	public synchronized void runGC() {
		if (!isEnabled()) {
			throw new UnsupportedOperationException(
					"Cannot run GC because mBean is not enabled.");
		}
		if (gcController == null) {
			throw new RuntimeException("Failure: see log for more information");
		}
		if (gcController.gcEnabledInConfig()) {
			throw new UnsupportedOperationException(
					"Cannot run GC externally because GC is enabled through config.");
		}
		if (!gcController.isGCStarted()) {
			throw new UnsupportedOperationException(
					"Cannot run GC externally because GC is disabled either since this server "
							+ "is in PASSIVE state");
		}
		// XXX::Note:: There is potencially a rare here, one could check to see
		// if it is disabled and before GC is started it could be disabled. In
		// which case it will not be run, just logged in the log file.
		if (gcController.isGCDisabled()) {
			throw new UnsupportedOperationException(
					"Cannot run GC externally because GC is disabled in this server since another "
							+ "PASSIVE server is currently synching state with this ACTIVE server.");
		}
		if (isGCRunning()) {
			throw new UnsupportedOperationException(
					"Cannot run GC because GC is already running.");
		}

		Thread gcRunnerThread = new Thread(gcRunner);
		gcRunnerThread.setName("GCRunnerThread");
		gcRunnerThread.start();
	}

	public synchronized void reset() {
		// nothing to reset
	}

	public void registerGCController(GCComptroller controller) {
		if (isEnabled()) {
			if (gcController != null) {
				logger
						.warn("Registering new gc-controller while one already registered. Old : "
								+ gcController);
			}
			gcController = controller;
		}
	}

	public static interface GCComptroller {
		void startGC();

		boolean gcEnabledInConfig();

		boolean isGCDisabled();

		boolean isGCStarted();
	}

	static interface GCRunner extends Runnable {
		boolean isGCRunning();
	}
}
