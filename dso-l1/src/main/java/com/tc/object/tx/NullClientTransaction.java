/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

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
  protected void basicCreate(TCObject source) {
    // null do nothing
  }

  @Override
  protected void basicCreateRoot(String name, ObjectID rootID) {
    // null do nothing
  }

  @Override
  protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    // null do nothing
  }

  @Override
  protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters, LogicalChangeID id) {
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
  protected void basicArrayChanged(TCObject source, int startPos, Object array, int length) {
    // null do nothing
  }

  @Override
  public int getNotifiesCount() {
    return 0;
  }

  @Override
  protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue) {
    // do nothing
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

}
