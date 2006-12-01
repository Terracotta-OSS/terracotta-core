/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.util.Assert;

/**
 * An {@link IntConfigItem} that returns the sum of one or more other {@link IntConfigItem}s.
 */
public class SummingIntConfigItem implements IntConfigItem {

  private final IntConfigItem[] children;

  public SummingIntConfigItem(IntConfigItem[] children) {
    Assert.assertNoNullElements(children);
    this.children = children;
  }

  public int getInt() {
    long out = 0;
    for (int i = 0; i < children.length; ++i) {
      out += children[i].getInt();
    }

    // Watch for overflow.
    if (out > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    else return (int) out;
  }

  public Object getObject() {
    return new Integer(getInt());
  }

  public void addListener(ConfigItemListener changeListener) {
    for (int i = 0; i < this.children.length; ++i)
      children[i].addListener(changeListener);
  }

  public void removeListener(ConfigItemListener changeListener) {
    for (int i = 0; i < this.children.length; ++i)
      children[i].removeListener(changeListener);
  }

}
