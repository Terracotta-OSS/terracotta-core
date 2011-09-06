/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.walker;

import java.lang.reflect.Field;

public class MemberValue {

  private final Object value;
  private int          id         = -1;
  private boolean      isRepeated = false;
  private boolean      isMapKey   = false;
  private boolean      isMapValue = false;
  private boolean      isShadowed = false;
  private boolean      isElement  = false;
  private int          index;
  private FieldData    fd;

  static MemberValue fieldValue(FieldData fd, Object instance) {
    MemberValue rv = new MemberValue(fd.getValue(instance));
    rv.fd = fd;
    rv.isShadowed = fd.isShadowed();
    return rv;
  }

  static MemberValue rootValue(Object root) {
    return new MemberValue(root);
  }

  static MemberValue mapKey(Object o) {
    MemberValue rv = new MemberValue(o);
    rv.isMapKey = true;
    return rv;
  }

  static MemberValue mapValue(Object o) {
    MemberValue rv = new MemberValue(o);
    rv.isMapValue = true;
    return rv;
  }

  static MemberValue elementValue(int index, Object val) {
    MemberValue rv = new MemberValue(val);
    rv.index = index;
    rv.isElement = true;
    return rv;
  }

  protected MemberValue(Object value) {
    this.value = value;
  }

  public int getIndex() {
    return index;
  }

  public Field getSourceField() {
    if (fd == null) { return null; }
    return fd.getField();
  }

  public Object getValueObject() {
    return this.value;
  }

  public boolean isElement() {
    return isElement;
  }

  public boolean isRepeated() {
    return isRepeated;
  }

  public boolean isShadowed() {
    return isShadowed;
  }

  public boolean isMapKey() {
    return isMapKey;
  }

  public boolean isMapValue() {
    return isMapValue;
  }

  public int getId() {
    //if (id < 0) throw new AssertionError("id: " + id);
    return id;
  }

  void setId(int newId) {
    if (newId < 0) throw new AssertionError("id: " + newId);
    this.id = newId;
  }

  void setRepeated(boolean b) {
    this.isRepeated = b;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(", id="+id);
    if(value != null) sb.append(", value="+value);
    if(isMapKey()) sb.append(", mapKey");
    if(isMapValue()) sb.append(", mapValue");
    if(isRepeated()) sb.append(", repeated");
    if(isElement()) sb.append(", isElement");
    if(fd != null) sb.append(", field="+fd.getField());
    return sb.toString();
  }
}
