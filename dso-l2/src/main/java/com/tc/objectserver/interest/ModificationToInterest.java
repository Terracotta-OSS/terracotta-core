/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.google.common.base.Function;

/**
 * @author Eugene Shelestovich
 */
public enum ModificationToInterest implements Function<Modification, Interest> {

  FUNCTION;

  @Override
  public Interest apply(final Modification modification) {
    final Interest event;
    final ModificationType type = modification.getType();
    final Object key = modification.getKey();
    final byte[] value = modification.getValue();
    final String cacheName = modification.getCacheName();
    switch (type) {
      case PUT:
        event = new PutInterest(key, value, cacheName);
        break;
      case REMOVE:
        event = new RemoveInterest(key, cacheName);
        break;
      case EVICT:
        event = new EvictionInterest(key, cacheName);
        break;
      case EXPIRE:
        event = new ExpirationInterest(key, cacheName);
        break;
      default:
        throw new IllegalStateException("Unknown modification type: " + type);
    }
    return event;
  }
}
