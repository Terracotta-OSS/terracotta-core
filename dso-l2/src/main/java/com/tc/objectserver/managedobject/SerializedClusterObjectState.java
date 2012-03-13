/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SerializedClusterObjectState extends AbstractManagedObjectState {

  private static final TCLogger logger = TCLogging.getLogger(SerializedClusterObjectState.class);

  private final long            classID;

  private byte[]                value;

  public SerializedClusterObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final SerializedClusterObjectState other = (SerializedClusterObjectState) o;

    if (!Arrays.equals(this.value, other.value)) { return false; }

    return true;
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final PhysicalAction pa = cursor.getPhysicalAction();
      if (pa.isEntireArray()) {
        final Object array = pa.getObject();
        if (array instanceof byte[]) {
          this.value = (byte[]) array;
        } else {
          final String type = safeTypeName(array);
          logger.error("received array of type " + type + " -- ignoring it");
        }
      } else {
        logger.error("received physical action: " + pa + " -- ignoring it");
      }
    }
  }

  /**
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  @Override
  public boolean hasNoReferences() {
    return true;
  }

  private static String safeTypeName(final Object obj) {
    final String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    // The byte[] value field is not shown in the admin console
    return fields;
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addEntireArray(this.value);
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  public byte getType() {
    return ManagedObjectStateStaticConfig.SERIALIZED_CLUSTER_OBJECT.getStateObjectType();
  }

  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    if (this.value != null) {
      out.writeInt(this.value.length);
      out.write(this.value, 0, this.value.length);
    } else {
      out.writeInt(-1);
    }
  }

  static SerializedClusterObjectState readFrom(final ObjectInput in) throws IOException {
    final SerializedClusterObjectState state = new SerializedClusterObjectState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException {

    final int length = in.readInt();
    if (length >= 0) {
      final byte[] data = new byte[length];
      in.read(data, 0, length);
      this.value = data;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + Arrays.hashCode(value);
    return result;
  }

}