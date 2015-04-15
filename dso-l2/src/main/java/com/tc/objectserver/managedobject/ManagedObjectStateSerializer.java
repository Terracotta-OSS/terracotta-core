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

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.api.Serializer;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ManagedObjectStateSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    if (!(o instanceof ManagedObjectState)) throw new AssertionError("Attempt to serialize an unknown type: " + o);
    ManagedObjectState mo = (ManagedObjectState) o;
    out.writeByte(mo.getType());
    mo.writeTo(out);
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    byte type = in.readByte();
    return getStateFactory().readManagedObjectStateFrom(in, type);
  }

  @Override
  public byte getSerializerID() {
    return MANAGED_OBJECT_STATE;
  }

  private ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

}
