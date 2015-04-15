/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.api.Serializer;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.ManagedObjectPersistor;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ManagedObjectSerializer implements Serializer {
  private final ManagedObjectStateSerializer serializer;
  private final ManagedObjectPersistor persistor;

  public ManagedObjectSerializer(ManagedObjectStateSerializer serializer, ManagedObjectPersistor persistor) {
    this.serializer = serializer;
    this.persistor = persistor;
  }

  @Override
  public void serializeTo(final Object mo, final ObjectOutput out) throws IOException {
    if (mo instanceof ManagedObject) {
      ((ManagedObject)mo).serializeTo(out, serializer);
    } else {
      throw new IllegalArgumentException("Trying to serialize a non-ManagedObject " + mo);
    }
  }

  @Override
  public Object deserializeFrom(final ObjectInput in) throws IOException {
    // read data
    final long version = in.readLong();
    final ObjectID id = new ObjectID(in.readLong());
    final ManagedObjectState state = (ManagedObjectState) this.serializer.deserializeFrom(in);

    // populate managed object...
    final ManagedObjectImpl rv = new ManagedObjectImpl(id, persistor);
    rv.setDeserializedState(version, state);
    return rv;
  }

  @Override
  public byte getSerializerID() {
    return MANAGED_OBJECT;
  }
}
