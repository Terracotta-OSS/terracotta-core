/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * state for maps
 */
public class LinkedHashMapManagedObjectState extends PartialMapManagedObjectState {
  private static final String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";
  private boolean             accessOrder            = false;

  // TODO:: Come back and make this partial too
  LinkedHashMapManagedObjectState(long classID) {
    super(classID, new LinkedHashMap(1));
  }

  protected LinkedHashMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    if (!cursor.next()) { return; }
    Object action = cursor.getAction();
    if (action instanceof PhysicalAction) {
      PhysicalAction physicalAction = (PhysicalAction) action;
      Assert.assertEquals(ACCESS_ORDER_FIELDNAME, physicalAction.getFieldName());
      setAccessOrder(physicalAction.getObject());
    } else {
      LogicalAction logicalAction = (LogicalAction) action;
      int method = logicalAction.getMethod();
      Object[] params = logicalAction.getParameters();
      applyMethod(objectID, includeIDs, method, params);
    }

    while (cursor.next()) {
      LogicalAction logicalAction = cursor.getLogicalAction();
      int method = logicalAction.getMethod();
      Object[] params = logicalAction.getParameters();
      applyMethod(objectID, includeIDs, method, params);
    }
  }

  @Override
  protected void applyMethod(ObjectID objectID, ApplyTransactionInfo includeIDS, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.GET:
        ((LinkedHashMap) references).get(params[0]);
        break;
      default:
        super.applyMethod(objectID, includeIDS, method, params);
    }
  }

  private void setAccessOrder(Object accessOrderObject) {
    try {
      Assert.assertTrue(accessOrderObject instanceof Boolean);
      accessOrder = ((Boolean) accessOrderObject).booleanValue();
      references = new LinkedHashMap(1, 0.75f, accessOrder);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    writer.addPhysicalAction(ACCESS_ORDER_FIELDNAME, Boolean.valueOf(accessOrder));
    super.dehydrate(objectID, writer, type);
  }

  // TODO: The Facade does not include the access order.
  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    return super.createFacade(objectID, className, limit);
  }

  @Override
  public byte getType() {
    return LINKED_HASHMAP_TYPE;
  }

  // TODO:: Until partial collections support is enabled for this class
  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeBoolean(accessOrder);
    out.writeInt(references.size());
    for (Iterator i = references.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  // TODO:: Until CollectionsPersistor saves retrieves data in references map.
  static MapManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    LinkedHashMapManagedObjectState linkedsmo = new LinkedHashMapManagedObjectState(in);
    linkedsmo.accessOrder = in.readBoolean();
    int size = in.readInt();
    LinkedHashMap map = new LinkedHashMap(size, 0.75f, linkedsmo.accessOrder);
    for (int i = 0; i < size; i++) {
      Object key = in.readObject();
      Object value = in.readObject();
      map.put(key, value);
    }
    linkedsmo.setMap(map);
    return linkedsmo;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (accessOrder ? 1231 : 1237);
    return result;
  }
}