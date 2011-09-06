/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Map.Entry;

class MapEntry extends MemberValue {

  private final Entry entry;
  private final int   index;

  public MapEntry(Entry entry, int index) {
    super(entry);
    this.entry = entry;
    this.index = index;
  }

  public Object getKey() {
    return entry.getKey();
  }

  public Object getValue() {
    return entry.getValue();
  }

  public int getIndex() {
    return index;
  }

}
