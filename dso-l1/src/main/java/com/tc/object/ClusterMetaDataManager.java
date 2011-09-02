/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.locks.ThreadID;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ClusterMetaDataManager extends ClientHandshakeCallback {

  public DNAEncoding getEncoding();

  public Set<NodeID> getNodesWithObject(ObjectID id);

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects(Collection<ObjectID> ids);

  public Set<?> getKeysForOrphanedValues(TCMap tcMap);

  public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node);

  public void setResponse(ThreadID threadId, Object response);

  public <K> Map<K, Set<NodeID>> getNodesWithKeys(TCMap tcMap, Collection<? extends K> keys);

  public <K> Map<K, Set<NodeID>> getNodesWithKeys(TCServerMap tcMap, Collection<? extends K> keys);
}