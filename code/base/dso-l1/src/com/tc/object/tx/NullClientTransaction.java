/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.Notify;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NullClientTransaction extends AbstractClientTransaction {

  public NullClientTransaction() {
    super();
  }

  public boolean hasChangesOrNotifies() {
    return false;
  }

  public boolean hasChanges() {
    return false;
  }

  public Map getNewRoots() {
    return Collections.EMPTY_MAP;
  }

  protected void basicCreate(TCObject source) {
    // null do nothing
  }

  protected void basicCreateRoot(String name, ObjectID rootID) {
    // null do nothing
  }

  protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    // null do nothing
  }

  protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters) {
    // null do nothing
  }

  public boolean isNull() {
    return true;
  }

  public Map getChangeBuffers() {
    return Collections.EMPTY_MAP;
  }

  public void addNotify(Notify notify) {
    return;
  }

  public boolean isConcurrent() {
    return false;
  }

  protected void basicArrayChanged(TCObject source, int startPos, Object array, int length) {
    // null do nothing
  }

  public int getNotifiesCount() {
    return 0;
  }

  protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue) {
    // do nothing
  }

  public Collection getReferencesOfObjectsInTxn() {
    return Collections.EMPTY_LIST;
  }

  public void addDmiDescriptor(DmiDescriptor dd) {
    return;
  }

  public boolean hasDmiDescriptors() {
    return false;
  }

  public List getDmiDescriptors() {
    return Collections.EMPTY_LIST;
  }

  public List getNotifies() {
    return Collections.EMPTY_LIST;
  }

}
