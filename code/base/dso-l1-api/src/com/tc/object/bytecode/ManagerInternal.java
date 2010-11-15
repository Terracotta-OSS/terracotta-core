/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.locks.TerracottaLockingInternal;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.NVPair;
import com.tc.search.SearchQueryResults;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ManagerInternal extends Manager, TerracottaLockingInternal {

  MetaDataDescriptor createMetaDataDescriptor(String category);

  public SearchQueryResults executeQuery(String cachename, LinkedList queryStack, boolean includeKeys,
                                         Set<String> attributeSet, Map<String, SortOperations> sortAttributeMap,
                                         List<NVPair> aggregators);

  public NVPair createNVPair(String name, Object value);

}
