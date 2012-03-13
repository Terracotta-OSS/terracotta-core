/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.FacadeUtil;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.MapEntryFacadeImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ToolkitTypeRootManagedObjectState extends LogicalManagedObjectState {
  private final Map references = new HashMap();

  public ToolkitTypeRootManagedObjectState(long classID) {
    super(classID);
  }

  public ToolkitTypeRootManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException {
    while (cursor.next()) {
      final LogicalAction action = cursor.getLogicalAction();
      final int method = action.getMethod();
      final Object[] params = action.getParameters();
      applyMethod(objectID, applyInfo, method, params);
    }
  }

  protected void applyMethod(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                             final Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        final Object key = params[0];
        final Object value = params[1];
        this.references.put(key, value);
        addChangeToCollector(objectID, key, applyInfo);
        addChangeToCollector(objectID, value, applyInfo);
        break;
      case SerializationUtil.REMOVE:
        this.references.remove(params[0]);
        break;
      default:
        throw new AssertionError("Invalid action:" + method);
    }
  }

  protected void addChangeToCollector(ObjectID objectID, Object newValue, ApplyTransactionInfo includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
      includeIDs.addBackReference((ObjectID) newValue, objectID);
    }
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    for (final Iterator i = this.references.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final Object key = entry.getKey();
      final Object value = entry.getValue();
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { key, value });
    }
  }

  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = this.references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    final MapEntryFacade[] data = new MapEntryFacade[limit];

    int index = 0;

    for (final Iterator i = this.references.entrySet().iterator(); i.hasNext() && index < limit; index++) {
      final Entry entry = (Entry) i.next();
      final Object key = FacadeUtil.processValue(entry.getKey());
      final Object value = FacadeUtil.processValue(entry.getValue());
      data[index] = new MapEntryFacadeImpl(key, value);
    }

    return LogicalManagedObjectFacade.createMapInstance(objectID, className, data, size);
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.TOOLKIT_TYPE_ROOT.getStateObjectType();
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    // ADD nothing
    // L1s wont fault anything unless required
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(references.size());
    for (final Iterator i = this.references.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final Object key = entry.getKey();
      final Object value = entry.getValue();
      out.writeObject(key);
      out.writeObject(value);
    }
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    ToolkitTypeRootManagedObjectState mo = (ToolkitTypeRootManagedObjectState) o;
    return references.equals(mo.references);
  }

  static ToolkitTypeRootManagedObjectState readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
    ToolkitTypeRootManagedObjectState ttrmo = new ToolkitTypeRootManagedObjectState(in);
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      ttrmo.references.put(in.readObject(), in.readObject());
    }
    return ttrmo;
  }

}
