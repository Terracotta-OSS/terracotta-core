/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.tc.object.ObjectID;

public interface ValuesResolver<K, V> {

  V get(K key, ObjectID valueOid);
}
