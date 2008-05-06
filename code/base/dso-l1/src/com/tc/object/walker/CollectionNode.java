/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Collection;
import java.util.Iterator;

public class CollectionNode extends PlainNode {

  private final Iterator entryIterator;
  private int            index = 0;

  protected CollectionNode(Collection c, WalkTest walkTest) {
    super(c, walkTest);
    entryIterator = c.iterator();
  }

  public boolean done() {
    return super.done() && !entryIterator.hasNext();
  }

  public MemberValue next() {
    if (!super.done()) {
      return super.next();
    } else {
      return MemberValue.elementValue(index++, entryIterator.next());
    }
  }

}
