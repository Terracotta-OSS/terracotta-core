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
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;
import org.terracotta.corestorage.StorageManager;

import com.tc.net.ClientID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

/**
 * @author tim
 */
public class TransactionPersistorImpl implements TransactionPersistor {
  private static final String                                                     TRANSACTION = "transaction";

  private final KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public TransactionPersistorImpl(StorageManager storageManager) {
    this.committed = storageManager.getKeyValueStorage(TRANSACTION, GlobalTransactionID.class,
                                                       GlobalTransactionDescriptor.class);
  }

  public static void addConfigsTo(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
    configMap.put(TRANSACTION,
                  ImmutableKeyValueStorageConfig.builder(GlobalTransactionID.class, GlobalTransactionDescriptor.class)
                      .valueTransformer(GlobalTransactionDescriptorSerializer.INSTANCE)
                      .keyTransformer(GlobalTransactionIDSerializer.INSTANCE).build());
  }

  @Override
  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return committed.values();
  }

  @Override
  public void saveGlobalTransactionDescriptor(GlobalTransactionDescriptor gtx) {
    committed.put(gtx.getGlobalTransactionID(), gtx);
  }

  @Override
  public void deleteAllGlobalTransactionDescriptors(SortedSet<GlobalTransactionID> globalTransactionIDs) {
    committed.removeAll(globalTransactionIDs);
  }

  private static class GlobalTransactionIDSerializer extends AbstractIdentifierTransformer<GlobalTransactionID> {
    static final GlobalTransactionIDSerializer INSTANCE = new GlobalTransactionIDSerializer();

    GlobalTransactionIDSerializer() {
      super(GlobalTransactionID.class);
    }

    @Override
    protected GlobalTransactionID createIdentifier(final long id) {
      return new GlobalTransactionID(id);
    }
  }

  static class GlobalTransactionDescriptorSerializer extends Serializer<GlobalTransactionDescriptor> {
    static final GlobalTransactionDescriptorSerializer INSTANCE = new GlobalTransactionDescriptorSerializer();

    @Override
    public GlobalTransactionDescriptor recover(final ByteBuffer buffer) {
      GlobalTransactionID gid = new GlobalTransactionID(buffer.getLong());
      ServerTransactionID sid = new ServerTransactionID(new ClientID(buffer.getLong()),
                                                        new TransactionID(buffer.getLong()));
      GlobalTransactionDescriptor globalTransactionDescriptor = new GlobalTransactionDescriptor(sid, gid);
      int changeResultSize = buffer.getInt();
      if (changeResultSize > 0) {
        Map<LogicalChangeID, LogicalChangeResult> changeResults = new HashMap<LogicalChangeID, LogicalChangeResult>(
                                                                                                                    changeResultSize);
        for (int i = 0; i < changeResultSize; i++) {
          LogicalChangeID id = new LogicalChangeID(buffer.getLong());
          LogicalChangeResult result = new LogicalChangeResult(buffer.get() != 0);
          changeResults.put(id, result);
        }
        globalTransactionDescriptor.recordLogicalChangeResults(changeResults);
      }
      return globalTransactionDescriptor;
    }

    @Override
    public ByteBuffer transform(final GlobalTransactionDescriptor globalTransactionDescriptor) {
      Map<LogicalChangeID, LogicalChangeResult> changeResults = globalTransactionDescriptor.getApplyResults();
      int bufferSize = Long.SIZE / Byte.SIZE * 3 + Integer.SIZE / Byte.SIZE * 1
                       + (changeResults.size() * (Long.SIZE / Byte.SIZE + Byte.SIZE));

      ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
      buffer.putLong(globalTransactionDescriptor.getGlobalTransactionID().toLong());
      buffer.putLong(((ClientID) globalTransactionDescriptor.getServerTransactionID().getSourceID()).toLong());
      buffer.putLong(globalTransactionDescriptor.getClientTransactionID().toLong());

      buffer.putInt(changeResults.size());
      for (Entry<LogicalChangeID, LogicalChangeResult> entry : changeResults.entrySet()) {
        buffer.putLong(entry.getKey().toLong());
        buffer.put(entry.getValue().isSuccess() ? (byte) 1 : (byte) 0);
      }
      buffer.flip();
      return buffer;
    }

    @Override
    public boolean equals(final GlobalTransactionDescriptor left, final ByteBuffer right) {
      return left.equals(recover(right));
    }
  }
}
