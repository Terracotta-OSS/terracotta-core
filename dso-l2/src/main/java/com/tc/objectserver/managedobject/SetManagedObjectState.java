/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import org.terracotta.corestorage.KeyValueStorage;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.objectserver.api.Destroyable;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.ObjectNotFoundException;
import com.tc.objectserver.persistence.PersistentObjectFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class SetManagedObjectState extends LogicalManagedObjectState implements Destroyable {
  protected final KeyValueStorage<Object, Object> references;
  private final ObjectID oid;

  SetManagedObjectState(long classID, ObjectID oid, PersistentObjectFactory objectFactory) {
    super(classID);
    this.oid = oid;
    try {
      this.references = objectFactory.getKeyValueStorage(oid, true);
    } catch (ObjectNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  protected SetManagedObjectState(ObjectInput in, PersistentObjectFactory objectFactory) throws IOException {
    super(in);
    this.oid = new ObjectID(in.readLong());
    this.references = objectFactory.getKeyValueStorage(oid, false);
  }

  @Override
  protected LogicalChangeResult applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo,
                                                   final int method,
                                    final Object[] params) {
    switch (method) {
      case SerializationUtil.ADD:
        Object v = params[0];
        addChangeToCollector(objectID, v, applyInfo);
        references.put(v, true);
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.REMOVE:
        references.remove(params[0]);
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.REMOVE_ALL:
        references.removeAll(Arrays.asList(params));
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.CLEAR:
        references.clear();
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.DESTROY:
        references.clear();
        return LogicalChangeResult.SUCCESS;
      default:
        throw new AssertionError("Invalid action:" + method);
    }
  }

  private void addChangeToCollector(ObjectID objectID, Object newValue, ApplyTransactionInfo includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
      includeIDs.addBackReference((ObjectID) newValue, objectID);
    }
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    for (Object o : references.keySet()) {
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { o });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    for (Object o : references.keySet()) {
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
  }

  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = (int) references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    Object[] data = new Object[limit];

    int index = 0;
    for (Iterator iter = references.keySet().iterator(); iter.hasNext() && index < limit; index++) {
      data[index] = iter.next();
    }

    return LogicalManagedObjectFacade.createSetInstance(objectID, className, data, size);
  }

  @Override
  public byte getType() {
    return SET_TYPE;
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeLong(oid.toLong());
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    SetManagedObjectState mo = (SetManagedObjectState) o;
    return references.equals(mo.references);
  }

  static SetManagedObjectState readFrom(ObjectInput in, PersistentObjectFactory objectFactory) throws IOException {
    return new SetManagedObjectState(in, objectFactory);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((references == null) ? 0 : references.hashCode());
    return result;
  }

  @Override
  public void destroy() {
    if (this.references instanceof Destroyable) {
      ((Destroyable) this.references).destroy();
    }
  }
}
