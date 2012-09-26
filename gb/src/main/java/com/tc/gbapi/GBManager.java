package com.tc.gbapi;

import java.io.File;
import java.util.concurrent.Future;

/**
 * @author tim
 */
public class GBManager {

	// no data
	// -> create fresh
	// -> fail
	// data exists
	// -> use
	// -> fail
	// -> delete

	public GBManager(File path, GBMapFactory factory) {
	}

	public GBManagerConfiguration getConfiguration() {
		return null;
	}

	public Future<Void> start() {
		return null;
	}

	public <K, V> void attachMap(String name, GBMap<K, V> map)
			throws IllegalStateException {
		// depending on the GBManager implementation, could fail and throw an
		// IllegalStateException
	}

	public void detachMap(String name) {
		// Detaches the map from the object manager, compaction should clear
		// this maps contents from disk eventually.
	}

	public <K, V> GBMap<K, V> getMap(String name, Class<K> keyClass,
			Class<V> valueClass) {
		// throws when called before start()
		return null;
	}

	public <K, V> GBCache<K, V> getCache(String name, Class<K> keyClass,
			Class<V> valueClass) {
		// throws when called before start()
		return null;
	}

	public void begin() {

	}

	public void commit() {

	}
}
