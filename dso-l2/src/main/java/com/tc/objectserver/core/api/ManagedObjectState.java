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
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * The internal state of a managed object.
 */
public interface ManagedObjectState {

  /*
   * If you are adding new State objects, you need to add Serialization support in ManagedObjectStateFactory The unit
   * test ManagedObjectStateSerializationTest will also need to have a test case for each of these types, which is
   * defined to be public static final. This will ensure that Serialization support is added to
   * ManagedObjectStateFactory.
   */
  public static final byte    MAP_TYPE               = 0x03;
  public static final byte    LITERAL_TYPE           = 0x06;
  public static final byte    LIST_TYPE              = 0x07;
  public static final byte    PARTIAL_MAP_TYPE       = 0x0d;
  // XXX: hack to get support various tims.
  public static final byte    TDC_SERIALIZED_ENTRY   = 0x12;
  public static final byte    TOOLKIT_TYPE_ROOT_TYPE = 0x14;
  public static final byte    MAX_TYPE               = 0x15;

  // /////////////////////////////////////////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////
  // Do NOT add any more types here - use ManagedObjectStateStaticConfig instead
  // /////////////////////////////////////////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException;

  public Set<ObjectID> getObjectReferences();

  public void addObjectReferencesTo(ManagedObjectTraverser traverser);

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type);

  public byte getType();

  public String getClassName();

  public void writeTo(ObjectOutput o) throws IOException;

  // The readFrom() method is currently a static implementation in each state object till I figure out
  // a cleaner way to create physical managed object as each one uses a different class object.
  // public void readFrom(ObjectInput i) throws IOException;
}
