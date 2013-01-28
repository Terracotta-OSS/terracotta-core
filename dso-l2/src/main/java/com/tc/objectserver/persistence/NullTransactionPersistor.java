package com.tc.objectserver.persistence;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

/**
 * @author tim
 */
public class NullTransactionPersistor implements TransactionPersistor {
  @Override
  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void saveGlobalTransactionDescriptor(final GlobalTransactionDescriptor gtx) {
    // Do nothing
  }

  @Override
  public void deleteAllGlobalTransactionDescriptors(final SortedSet<GlobalTransactionID> globalTransactionIDs) {
    // do nothing
  }
}
