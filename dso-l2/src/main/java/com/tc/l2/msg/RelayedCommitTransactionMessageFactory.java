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
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RelayedCommitTransactionMessageFactory {

  public static RelayedCommitTransactionMessage createRelayedCommitTransactionMessage(
                                                                                      final NodeID nodeID,
                                                                                      final TCByteBuffer[] data,
                                                                                      final Collection txns,
                                                                                      final long seqID,
                                                                                      final GlobalTransactionID lowWaterMark,
                                                                                      final ObjectStringSerializer serializer) {
    final RelayedCommitTransactionMessage msg = new RelayedCommitTransactionMessage(
                                                                                    nodeID,
                                                                                    data,
                                                                                    serializer,
                                                                                    getGlobalTransactionIDMapping(txns),
                                                                                    seqID, lowWaterMark);
    return msg;
  }

  private static Map getGlobalTransactionIDMapping(final Collection txns) {
    final Map sid2gid = new HashMap(txns.size());
    for (final Iterator i = txns.iterator(); i.hasNext();) {
      final ServerTransaction txn = (ServerTransaction) i.next();
      sid2gid.put(txn.getServerTransactionID(), txn.getGlobalTransactionID());
    }
    return sid2gid;
  }

}
