package com.tc.gbapi;

public interface GBCache<K, V> extends GBMap<K, V>{
	void addEvictionListener(GBCacheEvictionListener<K, V> listener);
	
	// Required for Ehcache
	boolean containsKeyOnHeap(K k);
	boolean containsKeyOffHeap(K k);
	
	void recalculateSize(K key);
}
