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

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

public class ToolkitNotifierManagedObjectState extends AbstractManagedObjectState {
  private final long classID;

  public ToolkitNotifierManagedObjectState(final long classID) {
    this.classID = classID;
  }

  /**
   * This apply method is empty because it doesnt need to do anything. The server doesnt maintain any state. All we care
   * is to see that the server broadcasts messages to the Clients.
   */
  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException {
    //
  }

  @Override
  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    //
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.TOOLKIT_NOTIFIER.getStateObjectType();
  }

  @Override
  public String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  @Override
  public void writeTo(ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
  }

  @Override
  protected boolean basicEquals(AbstractManagedObjectState o) {
    if (o instanceof ToolkitNotifierManagedObjectState) {
      return true;
    } else {
      return false;
    }
  }

  public static ToolkitNotifierManagedObjectState readFrom(ObjectInput in) throws IOException {
    final ToolkitNotifierManagedObjectState state = new ToolkitNotifierManagedObjectState(in.readLong());
    return state;
  }

}
