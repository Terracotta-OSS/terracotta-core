package com.tctest;

import java.lang.reflect.Field;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.commons.collections.FastHashMap;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public final class FastHashMapTestApp extends
		AbstractErrorCatchingTransparentApp {

	static final int EXPECTED_THREAD_COUNT = 2;

	private final CyclicBarrier barrier;

	private final FastHashMap clusteredFastHashMap;

	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
		config.addNewModule("clustered-commons-collections-3.1", "1.0.0");

		final String testClass = FastHashMapTestApp.class.getName();
		config.addIncludePattern(testClass + "$*");
		
		final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
		spec.addRoot("barrier", "barrier");
		spec.addRoot("clusteredFastHashMap", "clusteredFastHashMap");
	}

	public FastHashMapTestApp(final String appId, final ApplicationConfig cfg,
			final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		barrier = new CyclicBarrier(getParticipantCount());
		clusteredFastHashMap = new FastHashMap();
		clusteredFastHashMap.setFast(true);
	}

	protected void runTest() throws Throwable {
		if (barrier.await() == 0) {
			addDataToMap(2);
			letOtherNodeProceed();
			waitForPermissionToProceed();
			verifyEntries(4);
			removeDataFromMap(2);
			letOtherNodeProceed();
			waitForPermissionToProceed();
			verifyEntries(0);
		} else {
			waitForPermissionToProceed();
			verifyEntries(2);
			addDataToMap(2);
			letOtherNodeProceed();
			waitForPermissionToProceed();
			verifyEntries(2);
			clusteredFastHashMap.clear();
			letOtherNodeProceed();
		}
		barrier.await();
	}

	// This is lame but it makes runTest() slightly more readable
	private void letOtherNodeProceed() throws InterruptedException,
			BrokenBarrierException {
		barrier.await();
	}

	// This is lame but it makes runTest() slightly more readable
	private void waitForPermissionToProceed() throws InterruptedException,
			BrokenBarrierException {
		barrier.await();
	}

	private void addDataToMap(final int count) {
		for (int pos = 0; pos < count; ++pos) {
			clusteredFastHashMap.put(new Object(), new Object());
		}
	}

	private void removeDataFromMap(final int count) {
		for (int pos = 0; pos < count; ++pos) {
			clusteredFastHashMap.remove(clusteredFastHashMap.keySet()
					.iterator().next());
		}
	}

	private void verifyEntries(final int count) {
		Assert.assertEquals(count, clusteredFastHashMap.size());
		Assert.assertEquals(count, clusteredFastHashMap.keySet().size());
		Assert.assertEquals(count, clusteredFastHashMap.values().size());
	}

}
