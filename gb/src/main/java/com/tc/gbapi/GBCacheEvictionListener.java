package com.tc.gbapi;

public interface GBCacheEvictionListener<K, V> {
	public void notifyElementEvicted(K key, V value);
}
