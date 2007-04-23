package com.tctest;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class EhcacheManagerTestApp extends AbstractErrorCatchingTransparentApp {
	static final int EXPECTED_THREAD_COUNT = 2;

	private final CyclicBarrier barrier;

	private final CacheManager clusteredCacheManager;

	/**
	 * Test that Ehcache's CacheManger and Cache objects can be clustered.
	 * 
	 * @param appId
	 * @param cfg
	 * @param listenerProvider
	 */
	public EhcacheManagerTestApp(final String appId,
			final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		barrier = new CyclicBarrier(getParticipantCount());
		clusteredCacheManager = CacheManager.getInstance();
	}

	/**
	 * Inject Ehcache 1.2.4 configuration, and instrument this test class
	 * 
	 * @param visitor
	 * @param config
	 */
	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
		config.addNewModule("clustered-ehcache-1.2.4", "1.0.0");
		config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

		final String testClass = EhcacheManagerTestApp.class.getName();
		final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
		spec.addRoot("barrier", "barrier");
		spec.addRoot("clusteredCacheManager", "clusteredCacheManager");
	}

	/**
	 * Test that the data written in the clustered CacheManager by one node,
	 * becomes available in the other.
	 */
	protected void runTest() throws Throwable {
		final int CACHE_POPULATION = 10;

		if (barrier.await() == 0) {
			// create 2 caches, wait for the other node to verify 
			addCache("CACHE1", true);
			addCache("CACHE2", false);
			letOtherNodeProceed();
			
			// check that the first cache was removed
			waitForPermissionToProceed();
			verifyCacheRemoved("CACHE1");
			verifyCacheCount(1);
			letOtherNodeProceed();

			// check that the second cache was removed
			waitForPermissionToProceed();
			verifyCacheRemoved("CACHE2");
			verifyCacheCount(0);
			
			// add a bunch of caches, wait for the other node to verify
			addManyCaches(CACHE_POPULATION);
			letOtherNodeProceed();

			// check that the entire bunch was removed
			waitForPermissionToProceed();
			verifyCacheCount(0);

			// now shutdown the CacheManager, wait for the other node to verify
			shutdownCacheManager();
			letOtherNodeProceed();
		} else {
			// check that there are 2 caches
			waitForPermissionToProceed();
			verifyCacheCount(2);
			verifyCache("CACHE1");
			verifyCache("CACHE2");

			// remove the first cache, wait for the other node to verify
			removeCache("CACHE1");
			letOtherNodeProceed();

			// remove the second cache, wait for the other node to verify
			waitForPermissionToProceed();
			removeCache("CACHE2");
			letOtherNodeProceed();

			// check that a bunch of caches was created
			waitForPermissionToProceed();
			verifyCacheCount(CACHE_POPULATION);
			
			// now get rid of all of it, wait for the other node to verify
			removeAllCaches();
			letOtherNodeProceed();
			
			// check that the CacheManager was shutdown
			waitForPermissionToProceed();
			verifyCacheManagerShutdown();
		}
		barrier.await();
	}

	/**
	 * Create many caches.
	 * 
	 * @param count The number of caches to create
	 * @throws Throwable
	 */
	private void addManyCaches(final int count) throws Throwable {
		System.out.println(">> addManyCaches");
		for (int i = 0; i < count; i++) {
			addCache("MANYCACHE" + i, true);
		}
	}

	/**
	 * Remove all the caches.
	 */
	private void removeAllCaches() {
		System.out.println(">> removeAllCaches");
		clusteredCacheManager.removalAll();
	}

	/**
	 * Verify that we have an expected number of caches created.
	 * 
	 * @param expected
	 * @throws Exception
	 */
	private void verifyCacheCount(final int expected) throws Exception {
		System.out.println(">> verifyCacheCount");
		final String[] cacheNames = clusteredCacheManager.getCacheNames();
		Assert.assertEquals(expected, cacheNames.length);
	}

	/**
	 * Add a cache into the CacheManager.
	 * 
	 * @param name
	 *            The name of the cache to add
	 * @param mustDelegate
	 *            create Manually create the cache or let the manager handle the
	 *            details
	 * @throws Throwable
	 */
	private void addCache(final String name, boolean mustDelegate)
			throws Throwable {
		System.out.println(">> addCache");
		if (mustDelegate) {
			clusteredCacheManager.addCache(name);
		} else {
			Cache cache = new Cache(name, 2, false, true, 0, 2);
			clusteredCacheManager.addCache(cache);
		}
		Cache cache = clusteredCacheManager.getCache(name);
		cache.put(new Element(name + "key1", "value1"));
		cache.put(new Element(name + "key2", "value1"));
	}

	/**
	 * Verify that the named cache exists and that it's contents can be
	 * retrieved.
	 * 
	 * @param name
	 *            The name of the cache to retrieve
	 * @throws Exception
	 */
	private void verifyCache(final String name) throws Exception {
		System.out.println(">> verifyCache");
		boolean cacheExists = clusteredCacheManager.cacheExists(name);
		Assert.assertEquals(true, cacheExists);

		Cache cache = clusteredCacheManager.getCache(name);
		Assert.assertNotNull(cache);
		Assert.assertEquals(name, cache.getName());
		Assert.assertEquals(Status.STATUS_ALIVE, cache.getStatus());

		//int sizeFromGetSize = cache.getSize();
		//int sizeFromKeys = cache.getKeys().size();
		//Assert.assertEquals(sizeFromGetSize, sizeFromKeys);
		//Assert.assertEquals(2, cache.getSize());
	}

	/**
	 * Remove the named cache
	 * 
	 * @param name
	 */
	private void removeCache(final String name) {
		System.out.println(">> removeCache(" + name + ") : " + clusteredCacheManager.cacheExists(name));
		clusteredCacheManager.removeCache(name);
	}

	/**
	 * Verify that the named cache no longer exists.
	 * 
	 * @param name
	 * @throws Exception
	 */
	private void verifyCacheRemoved(final String name) throws Exception {
		System.out.println(">> verifyCacheRemoved");
		boolean cacheExists = clusteredCacheManager.cacheExists(name);
		Assert.assertEquals(false, cacheExists);

		Cache cache = clusteredCacheManager.getCache(name);
		Assert.assertNull(cache);
	}

	/**
	 * Shuts down the clustered cache manager.
	 */
	private void shutdownCacheManager() {
		System.out.println(">> shutdownCacheManager");
		clusteredCacheManager.shutdown();
	}

	/**
	 * Verify that the clustered cache manager has shut down.
	 */
	private void verifyCacheManagerShutdown() {
		System.out.println(">> verifyCacheManagerShutdown");
		Assert.assertEquals(Status.STATUS_SHUTDOWN, clusteredCacheManager
				.getStatus());
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
}
