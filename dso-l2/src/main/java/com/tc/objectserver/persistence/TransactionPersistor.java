package com.tc.objectserver.persistence;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;
import java.util.SortedSet;

/**
 * @author tim
 */
public interface TransactionPersistor {
  Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors();

  void saveGlobalTransactionDescriptor(GlobalTransactionDescriptor gtx);

  void deleteAllGlobalTransactionDescriptors(SortedSet<GlobalTransactionID> globalTransactionIDs);
}
