package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;
import org.terracotta.corestorage.StorageManager;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.impl.ServerMapEvictionTransactionBatchContext;
import com.tc.objectserver.tx.ActiveServerTransactionFactory;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderImpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EvictionTransactionPersistorImpl extends NullEvictionTransactionPersistorImpl {
  private static final TCLogger logger                          = TCLogging.getLogger(EvictionTransactionPersistorImpl.class);
  private static final String EVICTION_TRANSACTION              = "eviction-transaction";

  private final KeyValueStorage<ServerTransactionID, TransactionBatchContext> evictionTransactionStorage;

  private final PersistenceTransactionProvider persistenceTransactionProvider;

  public EvictionTransactionPersistorImpl(StorageManager storageManager, PersistenceTransactionProvider persistenceTransactionProvider) {
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.evictionTransactionStorage = storageManager.getKeyValueStorage(EVICTION_TRANSACTION, ServerTransactionID.class, TransactionBatchContext.class);
  }

  public static void addConfigsTo(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
    configMap.put(EVICTION_TRANSACTION, ImmutableKeyValueStorageConfig.builder(ServerTransactionID.class, TransactionBatchContext.class)
        .keyTransformer(ServerTransactionIDSerializer.INSTANCE)
        .valueTransformer(TransactionBatchContextSerializer.INSTANCE).build());
  }

  @Override
  public void saveTransactionBatch(ServerTransactionID serverTransactionID, TransactionBatchContext transactionBatchContext) {
    Transaction evictionTransaction = persistenceTransactionProvider.newTransaction();
    evictionTransactionStorage.put(serverTransactionID, transactionBatchContext);
    evictionTransaction.commit(); // commit to FRS
  }

  @Override
  public void removeTransaction(ServerTransactionID serverTransactionID) {
    if (evictionTransactionStorage.containsKey(serverTransactionID)) {
      logger.info("Removing server transaction id = " + serverTransactionID);
      evictionTransactionStorage.remove(serverTransactionID);
    }
  }

  @Override
  public Collection<TransactionBatchContext> getAllTransactionBatches() {
    return evictionTransactionStorage.values();
  }

  @Override
  public void removeAllTransactions() {
    evictionTransactionStorage.clear();
  }


  private static void fromByteBufferToTCByteBuffer(TCByteBuffer dest, ByteBuffer buf) {
    while (buf.hasRemaining()) {
      dest.put(buf.get());
    }
    dest.flip();
  }

  private static Byte[] toByteArray(TCByteBuffer[] tcByteBuffers) {
    List<Byte> byteList = new LinkedList<Byte>();
    ByteBuffer backingByteBuffer;
    for (TCByteBuffer tcb : tcByteBuffers) {
      backingByteBuffer = tcb.getNioBuffer().duplicate();
      while (backingByteBuffer.hasRemaining()) {
        byteList.add(backingByteBuffer.get());
      }
    }
    Byte[] ret = new Byte[byteList.size()];
    byteList.toArray(ret);
    return ret;
  }

  private abstract static class AbstractSerializer<T> extends Serializer<T> {

    @Override
    public boolean equals(T left, ByteBuffer right) throws IOException {
      return left.equals(recover(right));
    }

  }

  private static class ServerTransactionIDSerializer extends AbstractSerializer<ServerTransactionID> {

    static final ServerTransactionIDSerializer INSTANCE = new ServerTransactionIDSerializer();

    @Override
    public ServerTransactionID recover(ByteBuffer byteBuffer) throws IOException {
      Long transactionID = byteBuffer.getLong();
      int numPayloadBytesForServerID = byteBuffer.getInt();
      TCByteBuffer tcByteBuffer = TCByteBufferFactory.getInstance(false, numPayloadBytesForServerID);
      fromByteBufferToTCByteBuffer(tcByteBuffer, byteBuffer);

      TCByteBufferInput tcByteBufferInput = new TCByteBufferInputStream(tcByteBuffer);
      ServerID serverID = new ServerID();
      serverID.deserializeFrom(tcByteBufferInput);
      return new ServerTransactionID(serverID, new TransactionID(transactionID));
    }

    @Override
    public ByteBuffer transform(ServerTransactionID serverTransactionID) throws IOException {
      // ServerTransactionID = ServerID + TransactionID

      TCByteBufferOutputStream tcByteBufferOutputStream = new TCByteBufferOutputStream();
      (serverTransactionID.getSourceID()).serializeTo(tcByteBufferOutputStream);
      Byte[] serverIDByteArray = toByteArray(tcByteBufferOutputStream.toArray());

      //byte[] serverUID = (ServerID.NULL_ID).getUID();
      Long transactionID = serverTransactionID.getClientTransactionID().toLong();

      ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE + Integer.SIZE/Byte.SIZE + serverIDByteArray.length);
      buffer.putLong(transactionID);
      buffer.putInt(serverIDByteArray.length);
      for (byte byt : serverIDByteArray) {
        buffer.put(byt);
      }
      buffer.flip();
      return buffer;
    }

  }


  private static class TransactionBatchContextSerializer extends AbstractSerializer<TransactionBatchContext> {

    static final TransactionBatchContextSerializer INSTANCE = new TransactionBatchContextSerializer();
    @Override
    public TransactionBatchContext recover(ByteBuffer transformed) throws IOException {
      int numOSSBytes = transformed.getInt();
      int numTxnContextBytes = transformed.getInt();
      int numberOfBytesInPayload = numOSSBytes + numTxnContextBytes;

      TCByteBuffer tcByteBuffer = TCByteBufferFactory.getInstance(false, numberOfBytesInPayload);
      fromByteBufferToTCByteBuffer(tcByteBuffer, transformed);
      TCByteBufferInput tcByteBufferInput = new TCByteBufferInputStream(tcByteBuffer);
      ObjectStringSerializer objectStringSerializer = new ObjectStringSerializerImpl();
      objectStringSerializer.deserializeFrom(tcByteBufferInput);

      // move tcByteBuffer by numOSSBytes
      for (int i = 0 ; i < numOSSBytes; i++) {
        tcByteBuffer.get();
      }

      TransactionBatchReader transactionBatchReader = new TransactionBatchReaderImpl(new TCByteBuffer[] { tcByteBuffer.slice() },
          ServerID.NULL_ID, objectStringSerializer, new ActiveServerTransactionFactory(), null );

      NodeID nodeID = transactionBatchReader.getNodeID();
      ServerTransaction transaction = transactionBatchReader.getNextTransaction();

      TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(nodeID,
          transaction, transactionBatchReader.getSerializer());

      return batchContext;
    }

    @Override
    public ByteBuffer transform(TransactionBatchContext transactionBatchContext) throws IOException {

      Byte[] transactionBatchContextByteArray = toByteArray(transactionBatchContext.getBackingBuffers());
      TCByteBufferOutputStream tcByteBufferOutputStream = new TCByteBufferOutputStream();
      transactionBatchContext.getSerializer().serializeTo(tcByteBufferOutputStream);

      Byte[] objectStringSerializerByteArray = toByteArray(tcByteBufferOutputStream.toArray());

      int numOSSBytes = objectStringSerializerByteArray.length;
      int numTxnBatchContextBytes = transactionBatchContextByteArray.length;
      int payloadSize = numOSSBytes + numTxnBatchContextBytes;

      // Two extra integer to store total number of bytes of OSS and TxnContext
      ByteBuffer encodedOSSPlusTxnBatchContext = ByteBuffer.allocate(payloadSize +  2 * Integer.SIZE/Byte.SIZE);

      encodedOSSPlusTxnBatchContext.putInt(numOSSBytes);
      encodedOSSPlusTxnBatchContext.putInt(numTxnBatchContextBytes);

      for (Byte byt : objectStringSerializerByteArray) {
        encodedOSSPlusTxnBatchContext.put(byt);
      }

      for (Byte byt : transactionBatchContextByteArray) {
        encodedOSSPlusTxnBatchContext.put(byt);
      }

      encodedOSSPlusTxnBatchContext.flip();
      return encodedOSSPlusTxnBatchContext;
    }
  }

}
