/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.idprovider.api;

import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ClientTransaction;

/**
 * Responsible for generating the next unique objectID across processes in an efficient manner
 */
public interface ObjectIDProvider {

  public ObjectID next(ClientTransaction txn, Object pojo, GroupID gid);

  public void reserve(int size, GroupID gid);

}