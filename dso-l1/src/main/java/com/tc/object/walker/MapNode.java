/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.walker;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapNode extends PlainNode {

  private final Iterator entryIterator;
  private int            index = 0;

  public MapNode(Map map, WalkTest walkTest) {
    super(map, walkTest);
    entryIterator = map.entrySet().iterator();
  }

  public boolean done() {
    return super.done() && !entryIterator.hasNext();
  }

  public MemberValue next() {
    if (!super.done()) {
      return super.next();
    } else {
      Map.Entry entry = (Entry) entryIterator.next();
      return new MapEntry(entry, index++);
    }
  }

}
