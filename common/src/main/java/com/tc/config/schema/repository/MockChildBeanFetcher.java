/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
