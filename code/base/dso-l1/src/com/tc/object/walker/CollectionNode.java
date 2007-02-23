/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Collection;
import java.util.Iterator;

public class CollectionNode extends AbstractNode implements Node {

  private final Iterator iterator;
  private int            index = 0;

  protected CollectionNode(Collection c) {
    super(c);
    iterator = c.iterator();
  }

  public boolean done() {
    return !iterator.hasNext();
  }

  public MemberValue next() {
    return MemberValue.elementValue(index++, iterator.next());
  }

}
