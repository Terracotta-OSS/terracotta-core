/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock {@link ChildBeanFetcher}, for use in tests.
 */
public class MockChildBeanFetcher implements ChildBeanFetcher {

  private int         numGetChilds;
  private XmlObject[] returnedChildren;
  private int         returnedChildrenPos;
  private List        lastParents;

  public MockChildBeanFetcher() {
    this.returnedChildren = new XmlObject[] { null };
    this.lastParents = new ArrayList();

    reset();
  }

  public void reset() {
    this.numGetChilds = 0;
    this.returnedChildrenPos = 0;
    this.lastParents.clear();
  }

  public XmlObject getChild(XmlObject parent) {
    ++this.numGetChilds;
    this.lastParents.add(parent);
    XmlObject out = this.returnedChildren[this.returnedChildrenPos++];
    if (this.returnedChildrenPos >= this.returnedChildren.length) this.returnedChildrenPos = 0;
    return out;
  }

  public XmlObject getLastParent() {
    return (XmlObject) this.lastParents.get(this.lastParents.size() - 1);
  }
  
  public XmlObject[] getLastParents() {
    return (XmlObject[]) this.lastParents.toArray(new XmlObject[this.lastParents.size()]);
  }

  public int getNumGetChilds() {
    return numGetChilds;
  }

  public void setReturnedChild(XmlObject returnedChild) {
    setReturnedChildren(new XmlObject[] { returnedChild });
  }

  public void setReturnedChildren(XmlObject[] returnedChildren) {
    this.returnedChildren = returnedChildren;
  }

}
