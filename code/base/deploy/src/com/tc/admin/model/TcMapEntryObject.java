/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.dso.DSOObjectVisitor;
import com.tc.objectserver.mgmt.MapEntryFacade;

public class TcMapEntryObject extends AbstractTcObject implements IMapEntry {
  private final MapEntryFacade facade;
  private IObject              key;
  private IObject              value;
  private String               label;

  public TcMapEntryObject(ManagedObjectFacadeProvider facadeProvider, String name, MapEntryFacade facade, IObject parent) {
    super(facadeProvider, name, parent);
    this.facade = facade;
    initLabel();
  }

  protected TcMapEntryObject(TcMapEntryObject other) {
    super(other.facadeProvider, other.name, other.parent);
    this.facade = other.facade;
    initLabel();
  }

  public IObject newCopy() {
    return new TcMapEntryObject(this);
  }

  private void initLabel() {
    label = name + " (MapEntry)";
  }

  @Override
  public Object getFacade() {
    return facade;
  }

  public IObject getKey() {
    if (key == null) {
      key = getElement("key", facade.getKey());
    }
    return key;
  }

  public IObject getValue() {
    if (value == null) {
      value = getElement("value", facade.getValue());
    }
    return value;
  }

  private IObject getElement(String field, Object theValue) {
    try {
      return newObject(field, theValue, null);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return null;
  }

  @Override
  public String toString() {
    return label;
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitMapEntry(this);
  }

}
