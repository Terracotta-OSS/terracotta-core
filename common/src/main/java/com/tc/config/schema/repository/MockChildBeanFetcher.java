/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package com.tc.config.schema.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock {@link ChildBeanFetcher}, for use in tests.
 */
public class MockChildBeanFetcher implements ChildBeanFetcher {

  private int numGetChilds;
  private Object[] returnedChildren;
  private int returnedChildrenPos;
  private final List<Object> lastParents;

  public MockChildBeanFetcher() {
    this.returnedChildren = new Object[] { null };
    this.lastParents = new ArrayList<Object>();

    reset();
  }

  public void reset() {
    this.numGetChilds = 0;
    this.returnedChildrenPos = 0;
    this.lastParents.clear();
  }

  @Override
  public Object getChild(Object parent) {
    ++this.numGetChilds;
    this.lastParents.add(parent);
    Object out = this.returnedChildren[this.returnedChildrenPos++];
    if (this.returnedChildrenPos >= this.returnedChildren.length) this.returnedChildrenPos = 0;
    return out;
  }

  public Object getLastParent() {
    return this.lastParents.get(this.lastParents.size() - 1);
  }

  public Object[] getLastParents() {
    return this.lastParents.toArray(new Object[this.lastParents.size()]);
  }

  public int getNumGetChilds() {
    return numGetChilds;
  }

  public void setReturnedChild(Object returnedChild) {
    setReturnedChildren(new Object[] { returnedChild });
  }

  public void setReturnedChildren(Object[] returnedChildren) {
    this.returnedChildren = returnedChildren;
  }

}
