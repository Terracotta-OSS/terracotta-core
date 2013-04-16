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
    //TODO: add other types
    switch (type) {
      case PUT:
        event = new PutInterest(modification.getKey(), modification.getValue());
        break;
      case REMOVE:
        event = new RemoveInterest(modification.getKey());
        break;
      case EVICT:
        event = new EvictionInterest(modification.getKey());
        break;
      default:
        throw new IllegalStateException("Unknown modification type: " + type);
    }
    return event;
  }
}
