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
import com.tc.objectserver.api.Destroyable;
import com.tc.objectserver.mgmt.FacadeUtil;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.MapEntryFacadeImpl;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
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
    this.references = factory.getMap(id, true);
  }

  protected MapManagedObjectState(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in);
    this.factory = factory;
    this.id = new ObjectID(in.readLong());
    this.references = factory.getMap(id, false);
  }

  @Override
  protected void applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                                    final Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        final Object key = getKey(params);
        final Object value = getValue(params);
        Object old = references.get(key);
        this.references.put(key, value);
        if (key instanceof ObjectID) {
          final ObjectID v = (ObjectID) key;
          getListener().changed(objectID, null, v);
          addBackReferenceForKey(applyInfo, v, objectID);
        }
        if (value instanceof ObjectID) {
          final ObjectID v = (ObjectID) value;
          getListener().changed(objectID, null, v);
          addBackReferenceForValue(applyInfo, v, objectID);
        }
        if (old instanceof ObjectID) {
          removedValueFromMap(objectID, applyInfo, (ObjectID) old);
          if (value instanceof ObjectID) addKeyPresentForValue(applyInfo, (ObjectID) value);
        }
        break;
      case SerializationUtil.REMOVE:
        old = this.references.get(params[0]);
        this.references.remove(params[0]);
        if (old instanceof ObjectID) {
          removedValueFromMap(objectID, applyInfo, (ObjectID) old);
        }
        break;
      case SerializationUtil.CLEAR:
      case SerializationUtil.DESTROY:
        clearedMap(applyInfo, references.values());
        this.references.clear();
        break;

      default:
        throw new AssertionError("Invalid action:" + method);
    }

  }

  protected void clearedMap(ApplyTransactionInfo applyInfo, Collection values) {
    // Overridden by subclasses
  }

  protected void removedValueFromMap(ObjectID mapID, ApplyTransactionInfo applyInfo, ObjectID objectID) {
    // Overridden by subclasses
  }

  protected void addKeyPresentForValue(ApplyTransactionInfo applyInfo, ObjectID value) {
    // Overridden by subclasses
  }

  protected void addBackReferenceForKey(final ApplyTransactionInfo includeIDs, final ObjectID key, final ObjectID map) {
    includeIDs.addBackReference(key, map);
  }

  protected void addBackReferenceForValue(final ApplyTransactionInfo includeIDs, final ObjectID value,
                                          final ObjectID map) {
    includeIDs.addBackReference(value, map);
  }

  protected Object getKey(final Object[] params) {
    return params[0];
  }

  protected Object getValue(final Object[] params) {
    return params[1];
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    for (Object key : references.keySet()) {
      final Object value = references.get(key);
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
    out.writeLong(id.toLong());
  }

//
//  public Map getMap() {
//    return this.references;
//  }

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
    factory.destroyMap(id);
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

  public Map getPersistentCollection() {
    throw new UnsupportedClassVersionError();
  }
}
