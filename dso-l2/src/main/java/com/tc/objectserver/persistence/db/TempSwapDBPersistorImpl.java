/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.inmemory.InMemoryClassPersistor;
import com.tc.objectserver.persistence.inmemory.InMemoryClientStatePersistor;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistentMapStore;
import com.tc.objectserver.persistence.inmemory.InMemorySequenceProvider;
import com.tc.objectserver.persistence.inmemory.NullStringIndexPersistor;
import com.tc.objectserver.persistence.inmemory.NullTransactionPersistor;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.util.sequence.MutableSequence;

import java.io.File;

public class TempSwapDBPersistorImpl extends DBPersistorImpl {
  public TempSwapDBPersistorImpl(TCLogger logger, DBEnvironment env,
                                 SerializationAdapterFactory serializationAdapterFactory) throws TCDatabaseException {
    super(logger, env, serializationAdapterFactory);
  }

  public TempSwapDBPersistorImpl(final TCLogger logger, final DBEnvironment env,
                                 final SerializationAdapterFactory serializationAdapterFactory, final File l2DataPath,
                                 final ObjectStatsRecorder objectStatsRecorder) throws TCDatabaseException {
    super(logger, env, serializationAdapterFactory, l2DataPath, objectStatsRecorder);
  }

  @Override
  protected StringIndexPersistor createStringIndexPersistor() {
    return new NullStringIndexPersistor();
  }

  @Override
  protected ClassPersistor createClassPersistor(final TCLogger logger) {
    return new InMemoryClassPersistor();
  }

  @Override
  protected PersistentMapStore createPersistentMapStore(final TCLogger logger) {
    return new InMemoryPersistentMapStore();
  }

  @Override
  protected ClientStatePersistor createClientStatePersistor(final TCLogger logger) {
    return new InMemoryClientStatePersistor();
  }

  @Override
  protected TransactionPersistor createTransactionPersistor() {
    return new NullTransactionPersistor();
  }

  @Override
  protected MutableSequence createGlobalTransactionIDSequence(final TCLogger logger) {
    return new InMemorySequenceProvider();
  }

}
