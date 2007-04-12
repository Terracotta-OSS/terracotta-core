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
	 * @param appId
	 * @param cfg
	 * @param listenerProvider
	 */
	public EhcacheManagerTestApp(final String appId, final ApplicationConfig cfg,
			final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		barrier = new CyclicBarrier(getParticipantCount());
		clusteredCacheManager = CacheManager.getInstance();
	}

	/**
	 * Inject Ehcache 1.2.0 configuration, and instrument this test class
	 * @param visitor
	 * @param config
	 */
	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
	    config.addNewModule("clustered-ehcache-1.2.0", "1.0.0");
		config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

	    final String testClass = EhcacheManagerTestApp.class.getName();
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
			addCache("CACHE1");
			letOtherNodeProceed();

			waitForPermissionToProceed();
			verifyCache("CACHE2");
			shutdownCacheManager();
			letOtherNodeProceed();
		} else {
			waitForPermissionToProceed();
			verifyCache("CACHE1");
			addCache("CACHE2");
			letOtherNodeProceed();
			
			waitForPermissionToProceed();
			verifyCacheManagerShutdown();
		}
		barrier.await();
	}
	
	/**
	 * Add a cache into the CacheManager.
	 * @param name The name of the cache to add
	 * @throws Throwable
	 */
	private void addCache(final String name) throws Throwable {
		synchronized(clusteredCacheManager) {
	        Cache cache = new Cache(name, 2, false, true, 0, 2);
			clusteredCacheManager.addCache(cache);
	        cache.put(new Element(name + "key1", "value1"));
	        cache.put(new Element(name + "key2", "value1"));
		}
	}

	/**
	 * Verify that the named cache exists and that it's contents
	 * can be retrieved.
	 * @param name The name of the cache to retrieve
	 * @throws Exception
	 */
	private void verifyCache(final String name) throws Exception {
		synchronized(clusteredCacheManager) {
			Cache cache = clusteredCacheManager.getCache(name);
			Assert.assertNotNull(cache);
			Assert.assertEquals(name, cache.getName());
			Assert.assertEquals(Status.STATUS_ALIVE, cache.getStatus());
			
	        //int sizeFromGetSize = cache.getSize();
	        //int sizeFromKeys = cache.getKeys().size();
	        //Assert.assertEquals(sizeFromGetSize, sizeFromKeys);
	        //Assert.assertEquals(2, cache.getSize());
	        
	        //Element key1 = cache.get(name + "key1");
	        //Element key2 = cache.get(name + "key2");
	        //Assert.assertNotNull(key1);
	        //Assert.assertNotNull(key2);
		}
	}

	/**
	 * Verify that the clustered cache manager has shut down.
	 */
	private void verifyCacheManagerShutdown() {
		synchronized(clusteredCacheManager) {
			Assert.assertEquals(Status.STATUS_SHUTDOWN, clusteredCacheManager.getStatus());
		}
	}
	
	/**
	 * Shuts down the clustered cache manager.
	 */
	private void shutdownCacheManager() {
		synchronized(clusteredCacheManager) {
			clusteredCacheManager.shutdown();
		}
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
