/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.*;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ToolkitObjectStripeState extends AbstractManagedObjectState {

  private final long classID;

  private volatile Map<String, Object> configMap = new HashMap<String, Object>();
  private Object[] componentObjects;

  public ToolkitObjectStripeState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState obj) {
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    ToolkitObjectStripeState other = (ToolkitObjectStripeState) obj;
    if (classID != other.classID) return false;
    if (!Arrays.equals(componentObjects, other.componentObjects)) return false;
    if (configMap == null) {
      if (other.configMap != null) return false;
    } else if (!configMap.equals(other.configMap)) return false;
    return true;
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
          throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction pa = (PhysicalAction) action;
        if (pa.isEntireArray()) {
          this.componentObjects = (Object[]) pa.getObject();
        } else {
          configMap.put(pa.getFieldName(), pa.getObject());
        }
      } else {
        final LogicalAction logicalAction = (LogicalAction) action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyMethod(objectID, method, params);
      }
    }
  }

  private void applyMethod(ObjectID objectID, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        final Object key = params[0];
        final Object value = params[1];
        String keyAsString;
        if (key instanceof UTF8ByteDataHolder) {
          keyAsString = ((UTF8ByteDataHolder) key).asString();
        } else {
          keyAsString = (String) key;
        }
        configMap.put(keyAsString, value);
        break;
      default:
        throw new AssertionError("Gor unhandled logical action: objectId: " + objectID + ", method: " + method
                + ", params: " + Arrays.asList(params));
    }
  }

  @Override
  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>(), limit);
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields, int limit) {
    fields.putAll(configMap);

    if (componentObjects != null) {
      int size = componentObjects.length;
      if (limit < 0) {
        limit = size;
      } else {
        limit = Math.min(limit, size);
      }
      for (int i = 0; i < limit; i++) {
        fields.put("components[" + i + "/" + componentObjects.length + "]", componentObjects[i]);
      }
    } else {
      fields.put("components", "<Empty Array>");
    }
    return fields;
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    for (Map.Entry<String, Object> entry : configMap.entrySet()) {
      writer.addPhysicalAction(entry.getKey(), entry.getValue());
    }
    writer.addEntireArray(componentObjects);
  }

  @Override
  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  @Override
  public Set getObjectReferences() {
    ObjectIDSet set = new ObjectIDSet();
    if (componentObjects != null) {
      for (Object obj : componentObjects) {
        if (obj instanceof ObjectID) {
          ObjectID oid = (ObjectID) obj;
          if (!oid.isNull()) {
            set.add(oid);
          }
        }
      }
    }
    return set;
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.TOOLKIT_OBJECT_STRIPE.getStateObjectType();
  }

  @Override
  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    writeMap(configMap, out);
    if (this.componentObjects != null) {
      out.writeInt(this.componentObjects.length);
      for (Object obj : componentObjects) {
        out.writeObject(obj);
      }
    } else {
      out.writeInt(-1);
    }
  }

  private static void writeMap(final Map<String, Object> map, final ObjectOutput out) throws IOException {
    out.writeInt(map.size());
    for (Entry<String, Object> e : map.entrySet()) {
      out.writeUTF(e.getKey());
      out.writeObject(e.getValue());
    }
  }

  private static Map<String, Object> readMap(final ObjectInput in) throws ClassNotFoundException, IOException {
    HashMap<String, Object> rv = new HashMap<String, Object>();
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      String key = in.readUTF();
      Object value = in.readObject();
      rv.put(key, value);
    }
    return rv;
  }

  static ToolkitObjectStripeState readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
    final ToolkitObjectStripeState state = new ToolkitObjectStripeState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    configMap = readMap(in);
    final int length = in.readInt();
    if (length >= 0) {
      componentObjects = new Object[length];
      for (int i = 0; i < componentObjects.length; i++) {
        componentObjects[i] = in.readObject();
      }
    } else {
      componentObjects = new Object[0];
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + Arrays.hashCode(componentObjects);
    result = prime * result + ((configMap == null) ? 0 : configMap.hashCode());
    return result;
  }

}