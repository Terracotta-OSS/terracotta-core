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
import com.tc.objectserver.mgmt.FacadeUtil;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.MapEntryFacadeImpl;
import com.tc.objectserver.persistence.ObjectNotFoundException;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * state for maps
 */
public class MapManagedObjectState extends LogicalManagedObjectState implements PrettyPrintable,
    Destroyable {
  private final ObjectID id;
  protected final KeyValueStorage<Object, Object> references;
  private final PersistentObjectFactory factory;

  protected MapManagedObjectState(final long classID, ObjectID id, PersistentObjectFactory factory) {
    super(classID);
    this.factory = factory;
    this.id = id;
    try {
      this.references = factory.getKeyValueStorage(id, true);
    } catch (ObjectNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  protected MapManagedObjectState(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in);
    this.factory = factory;
    this.id = new ObjectID(in.readLong());
    this.references = factory.getKeyValueStorage(id, false);
  }

  protected ObjectID getId() {
    return id;
  }

  @Override
  protected LogicalChangeResult applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo,
                                                   final int method,
                                    final Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        applyPut(applyInfo, params);
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.REMOVE:
        applyRemove(applyInfo, params);
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.CLEAR:
      case SerializationUtil.DESTROY:
        applyClear(applyInfo);
        return LogicalChangeResult.SUCCESS;
      case SerializationUtil.NO_OP:
        return LogicalChangeResult.SUCCESS;
      default:
        throw new AssertionError("Invalid action:" + method);
    }
  }

  protected void addedReferences(ApplyTransactionInfo applyInfo, Object... objects) {
    for (Object o : objects) {
      addedReference(applyInfo, o);
    }
  }

  protected void addedReferences(ApplyTransactionInfo applyInfo, Collection<?> objects) {
    for (Object o : objects) {
      addedReference(applyInfo, o);
    }
  }

  protected void addedReference(ApplyTransactionInfo applyInfo, Object o) {
    if (o instanceof ObjectID) {
      ObjectID oid = (ObjectID)o;
      getListener().changed(getId(), null, oid);
      applyInfo.addBackReference(oid, getId());
    }
  }

  protected void removedReferences(ApplyTransactionInfo applyInfo, Object... objects) {
    for (Object object : objects) {
      removedReference(applyInfo, object);
    }
  }

  protected void removedReferences(ApplyTransactionInfo applyInfo, Collection<?> objects) {
    for (Object object : objects) {
      removedReference(applyInfo, object);
    }
  }

  protected void removedReference(ApplyTransactionInfo applyInfo, Object o) {
    
  }

  protected Object applyPut(ApplyTransactionInfo applyInfo, Object[] params) {
    Object key = params[0];
    Object value = params[1];
    Object old = references.get(key);
    references.put(key, value);

    addedReferences(applyInfo, key, value);
    removedReferences(applyInfo, old);

    return old;
  }

  protected Object applyRemove(ApplyTransactionInfo applyInfo, Object[] params) {
    Object key = params[0];
    Object old = get(key);
    references.remove(key);
    removedReferences(applyInfo, key, old);
    return old;
  }

  protected void applyClear(ApplyTransactionInfo applyInfo) {
    removedReferences(applyInfo, references.keySet());
    removedReferences(applyInfo, references.values());
    this.references.clear();
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    for (Object key : references.keySet()) {
      final Object value = get(key);
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { key, value });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(final Set refs) {
    for (Object o : references.values()) {
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
    for (Object o : references.keySet()) {
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out = out.println("MapManagedObjectState").duplicateAndIndent();
    out.indent().println("references: " + this.references);
    return rv;
  }

  @Override
  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, int limit) {

    final int size = (int)this.references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    final MapEntryFacade[] data = new MapEntryFacade[limit];

    int index = 0;

    for (final Iterator<Object> i = references.keySet().iterator(); i.hasNext() && index < limit; index++) {
      Object rawKey = i.next();
      final Object key = FacadeUtil.processValue(rawKey);
      final Object value = FacadeUtil.processValue(references.get(rawKey));
      data[index] = new MapEntryFacadeImpl(key, value);
    }

    return LogicalManagedObjectFacade.createMapInstance(objectID, className, data, size);
  }

  @Override
  protected void basicWriteTo(final ObjectOutput out) throws IOException {
    out.writeLong(getId().toLong());
  }

  // CollectionsPersistor will save retrieve data in references map.
  static MapManagedObjectState readFrom(final ObjectInput in, PersistentObjectFactory factory) throws IOException,
      ClassNotFoundException {
    // make warning go away
    if (false) { throw new ClassNotFoundException(); }
    return new MapManagedObjectState(in, factory);
  }

  @Override
  public byte getType() {
    return MAP_TYPE;
  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    final MapManagedObjectState mmo = (MapManagedObjectState) o;
    return this.references.equals(mmo.references);
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
    factory.destroyKeyValueStorage(getId());
  }

  /*
   * These methods are used only for ServerClusterMetaDataManagerImpl, might be good to get rid of them
   * and do this in a less leaky way.
   */
  @Deprecated
  public Set<Object> keySet() {
    return references.keySet();
  }

  @Deprecated
  public Object get(Object key) {
    return references.get(key);
  }

  @Deprecated
  public boolean containsKey(Object key) {
    return references.containsKey(key);
  }
}
