/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Iterator;

public class PlainNode extends AbstractNode {

  private Iterator fieldsIterator;

  protected PlainNode(Object o) {
    super(o);
    this.fieldsIterator = AllFields.getAllFields(o).getFields();
  }

  public boolean done() {
    return !fieldsIterator.hasNext();
  }

  public MemberValue next() {
    FieldData fd = (FieldData) fieldsIterator.next();
    return MemberValue.fieldValue(fd, getObject());
  }

}
