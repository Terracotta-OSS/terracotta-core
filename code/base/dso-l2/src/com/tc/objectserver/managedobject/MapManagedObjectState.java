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
import com.tc.objectserver.mgmt.FacadeUtil;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.MapEntryFacadeImpl;
import com.tc.objectserver.persistence.db.PersistableCollection;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * state for maps
 */
public class MapManagedObjectState extends LogicalManagedObjectState implements PrettyPrintable, PersistableObjectState {
  protected Map references;

  protected MapManagedObjectState(final long classID, final Map map) {
    super(classID);
    this.references = map;
  }

  protected MapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo)
      throws IOException {
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

        mapPreProcess(params);
        final Object key = getKey(params);
        final Object value = getValue(params);
        Object old = this.references.put(key, value);
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
          invalidateIfNeeded(applyInfo, (ObjectID) old);
        }
        break;
      case SerializationUtil.REMOVE:
        old = this.references.remove(params[0]);
        if (old instanceof ObjectID) {
          invalidateIfNeeded(applyInfo, (ObjectID) old);
        }
        break;
      case SerializationUtil.CLEAR:
        this.references.clear();
        break;
      default:
        throw new AssertionError("Invalid action:" + method);
    }

  }

  protected void invalidateIfNeeded(ApplyTransactionInfo applyInfo, ObjectID objectID) {
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
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[1] : params[0];
  }

  protected Object getValue(final Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[2] : params[1];
  }

  private void mapPreProcess(final Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    if (params.length == 3) {
      this.references.remove(params[0]);
    }
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    for (final Iterator i = this.references.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final Object key = entry.getKey();
      final Object value = entry.getValue();
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { key, value });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(final Set refs) {
    for (final Iterator i = this.references.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final Object key = entry.getKey();
      final Object value = entry.getValue();
      if (key instanceof ObjectID) {
        refs.add(key);
      }
      if (value instanceof ObjectID) {
        refs.add(value);
      }
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out = out.println("MapManagedObjectState").duplicateAndIndent();
    out.indent().println("references: " + this.references);
    return rv;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, int limit) {
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
  protected void basicWriteTo(final ObjectOutput out) throws IOException {
    // CollectionsPersistor will save retrieve data in references map.
    if (false) { throw new IOException(); }
  }

  public void setMap(final Map map) {
    if (this.references != null) { throw new AssertionError("The references map is already set ! " + this.references); }
    this.references = map;
  }

  public Map getMap() {
    return this.references;
  }

  // CollectionsPersistor will save retrieve data in references map.
  static MapManagedObjectState readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
    if (false) {
      // This is added to make the compiler happy. For some reason if I have readFrom() method throw
      // ClassNotFoundException in LinkedHashMapManagedObjectState, it shows as an error !!
      throw new ClassNotFoundException();
    }
    return new MapManagedObjectState(in);
  }

  public byte getType() {
    return MAP_TYPE;
  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    final MapManagedObjectState mmo = (MapManagedObjectState) o;
    return this.references.equals(mmo.references);
  }

  public PersistableCollection getPersistentCollection() {
    return (PersistableCollection) getMap();
  }

  public void setPersistentCollection(final PersistableCollection collection) {
    setMap((Map) collection);
  }
}