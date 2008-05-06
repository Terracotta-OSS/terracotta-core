/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

public class MapEntryNode extends AbstractNode {

  private final Object key;
  private final Object value;
  private int          nextCount = 0;

  public MapEntryNode(MapEntry entry) {
    super(entry);
    key = entry.getKey();
    value = entry.getValue();
  }

  public boolean done() {
    return nextCount > 1;
  }

  public MemberValue next() {
    final MemberValue rv;

    switch (nextCount) {
      case 0: {
        rv = MemberValue.mapKey(key);
        break;
      }
      case 1: {
        rv = MemberValue.mapValue(value);
        break;
      }
      default:
        throw new IllegalStateException();
    }

    nextCount++;
    return rv;
  }

}
