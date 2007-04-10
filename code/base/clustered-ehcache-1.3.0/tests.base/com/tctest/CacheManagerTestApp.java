package com.tctest;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class CacheManagerTestApp extends AbstractErrorCatchingTransparentApp {
	static final int EXPECTED_THREAD_COUNT = 2;

	private final CyclicBarrier barrier;

    private final CacheManager clusteredCacheManager;

	/**
	 * 
	 * @param appId
	 * @param cfg
	 * @param listenerProvider
	 */
	public CacheManagerTestApp(final String appId, final ApplicationConfig cfg,
			final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		barrier = new CyclicBarrier(getParticipantCount());
		clusteredCacheManager = CacheManager.create();
	}

	/**
	 * Inject Ehcache 1.3.0 configuration, and instrument this test class
	 * @param visitor
	 * @param config
	 */
	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
	    config.addNewModule("clustered-ehcache-1.3.0", "1.0.0");
		config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

	    final String testClass = CacheManagerTestApp.class.getName();
		final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
		spec.addRoot("barrier", "barrier");
		spec.addRoot("clusteredCacheManager", "clusteredCacheManager");
	}
	
	/**
	 * Test that the data written in the clustered CacheManager
	 * by one node, becomes available in the other.
	 */
	protected void runTest() throws Throwable {
		if (barrier.await() == 0) {
			addDataToCache("DATA1");
			letOtherNodeProceed();
			waitForPermissionToProceed();
			verifyEntries("DATA2");
		} else {
			waitForPermissionToProceed();
			verifyEntries("DATA1");
			addDataToCache("DATA2");
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

	/**
	 * Add datum to CacheManager
	 * @param value The datum to add
	 * @throws Throwable
	 */
	private void addDataToCache(final String value) throws Throwable {
	}

	/**
	 * Attempt to retrieve datum from CacheManager 
	 * @param value The datum to retrieve
	 * @throws Exception
	 */
	private void verifyEntries(final String value) throws Exception {
	}
}
