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
package com.tc.object.tx;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NullClientTransaction extends AbstractClientTransaction {

  public NullClientTransaction() {
    super();
  }

  @Override
  public boolean hasChangesOrNotifies() {
    return false;
  }

  @Override
  public boolean hasChanges() {
    return false;
  }

  @Override
  public Map getNewRoots() {
    return Collections.EMPTY_MAP;
  }

  @Override
  protected void basicCreateRoot(String name, ObjectID rootID) {
    // null do nothing
  }

  @Override
  protected void basicLogicalInvoke(TCObject source, LogicalOperation method, Object[] parameters, LogicalChangeID id) {
    // null do nothing
  }

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public Map getChangeBuffers() {
    return Collections.EMPTY_MAP;
  }

  @Override
  public void addNotify(Notify notify) {
    return;
  }

  @Override
  public boolean isConcurrent() {
    return false;
  }

  @Override
  public int getNotifiesCount() {
    return 0;
  }

  @Override
  public Collection getReferencesOfObjectsInTxn() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public List getNotifies() {
    return Collections.EMPTY_LIST;
  }

  @Override
  protected void basicAddMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    // do nothing
  }

  @Override
  public int getSession() {
    return 0;
  }

  @Override
  protected void basicCreate(TCObject object) {
    //
  }

}
