/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Iterator;

class PlainNode extends AbstractNode {

  private Iterator fieldsIterator;

  protected PlainNode(Object o, WalkTest walkTest) {
    super(o);
    this.fieldsIterator = AllFields.getAllFields(o, true, walkTest).getFields();
  }

  public boolean done() {
    return !fieldsIterator.hasNext();
  }

  public MemberValue next() {
    FieldData fd = (FieldData) fieldsIterator.next();
    return MemberValue.fieldValue(fd, getObject());
  }

}
