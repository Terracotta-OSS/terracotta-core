/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.lang.reflect.Array;

public class ArrayNode extends AbstractNode implements Node {

  private final int length;
  private int       index = 0;

  protected ArrayNode(Object o) {
    super(o);
    length = Array.getLength(o);
  }

  public boolean done() {
    return index >= length;
  }

  public MemberValue next() {
    return MemberValue.elementValue(index, Array.get(getObject(), index++));
  }

}
