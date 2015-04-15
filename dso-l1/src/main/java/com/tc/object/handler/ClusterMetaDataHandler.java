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
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCDataInput;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessage;
import com.tc.object.msg.NodeMetaDataResponseMessage;
import com.tc.object.msg.NodesWithKeysResponseMessage;
import com.tc.object.msg.NodesWithObjectsResponseMessage;
import com.tcclient.cluster.DsoNodeMetaData;

import java.util.HashSet;
import java.util.Set;

public class ClusterMetaDataHandler extends AbstractEventHandler {

  private ClusterMetaDataManager clusterMetaDataManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof NodesWithObjectsResponseMessage) {
      handleNodesWithObjectsResponseMessage((NodesWithObjectsResponseMessage)context);
    } else if (context instanceof KeysForOrphanedValuesResponseMessage) {
      handleKeysForOrphanedValuesResponseMessage((KeysForOrphanedValuesResponseMessage)context);
    } else if (context instanceof NodeMetaDataResponseMessage) {
      handleNodeMetaDataResponseMessage((NodeMetaDataResponseMessage)context);
    } else if (context instanceof NodesWithKeysResponseMessage) {
      handleNodeMetaDataResponseMessage((NodesWithKeysResponseMessage)context);
    } else {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
  }

  private void handleNodeMetaDataResponseMessage(final NodesWithKeysResponseMessage message) {
    clusterMetaDataManager.setResponse(message.getThreadID(), message.getNodesWithKeys());
  }

  private void handleNodesWithObjectsResponseMessage(final NodesWithObjectsResponseMessage message) {
    clusterMetaDataManager.setResponse(message.getThreadID(), message.getNodesWithObjects());
  }

  private void handleKeysForOrphanedValuesResponseMessage(final KeysForOrphanedValuesResponseMessage message) {
    if (message.getOrphanedKeysDNA() != null) {
      final DNAEncoding encoding = clusterMetaDataManager.getEncoding();
      final TCDataInput input = new TCByteBufferInputStream(TCByteBufferFactory.wrap(message.getOrphanedKeysDNA()));
      final Set keys = new HashSet();
      try {
        final int size = input.readInt();
        for (int i = 0; i < size; i++) {
          keys.add(encoding.decode(input));
        }
      } catch (Exception e) {
        getLogger().error("Keys for orphaned values response decoding error: ", e);
      }

      clusterMetaDataManager.setResponse(message.getThreadID(), keys);
    } else {
      clusterMetaDataManager.setResponse(message.getThreadID(), message.getOrphanedValuesObjectIDs());
    }
  }

  private void handleNodeMetaDataResponseMessage(final NodeMetaDataResponseMessage message) {
    clusterMetaDataManager.setResponse(message.getThreadID(), new DsoNodeMetaData(message.getIp(), message.getHostname()));
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.clusterMetaDataManager = ccc.getClusterMetaDataManager();
  }

}
