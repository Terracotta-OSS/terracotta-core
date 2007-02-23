/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.walker;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapNode extends AbstractNode {

  private final Iterator iterator;

  private int            index = 0;

  public MapNode(Map map) {
    super(map);
    iterator = map.entrySet().iterator();
  }

  public boolean done() {
    return !iterator.hasNext();
  }

  public MemberValue next() {
    Map.Entry entry = (Entry) iterator.next();
    return new MapEntry(entry, index++);
  }

}
