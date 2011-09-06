/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.idprovider.impl;

import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.tx.ClientTransaction;
import com.tc.util.sequence.Sequence;

import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectIDProviderImpl implements ObjectIDProvider {

  private final Sequence        sequence;
  private final SortedSet<Long> cachedObjectIds = new TreeSet<Long>();

  public ObjectIDProviderImpl(Sequence sequence) {
    this.sequence = sequence;
  }

  public synchronized ObjectID next(ClientTransaction txn, Object pojo, GroupID gid) {
    long oidLong = -1;
    if (cachedObjectIds.size() > 0) {
      oidLong = this.cachedObjectIds.first();
      this.cachedObjectIds.remove(oidLong);
    } else {
      oidLong = this.sequence.next();
    }

    return new ObjectID(oidLong);
  }

  public synchronized void reserve(int size, GroupID gid) {
    int sizeNeeded = size - cachedObjectIds.size();
    for (int i = 0; i < sizeNeeded; i++) {
      cachedObjectIds.add(this.sequence.next());
    }
  }
}