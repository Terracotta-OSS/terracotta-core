/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import org.mockito.Mockito;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.InvalidateObjectsForClientContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.impl.InvalidateObjectManagerImpl;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.stats.Stats;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

public class InvalidateObjectManagerTest extends TCTestCase {
  private InvalidateObjectManagerImpl invalidateObjectManager;
  private TestSinkAdaptor             validateSink;
  private TestSinkAdaptor             inValidateSink;
  private ConfigurationContext        configurationContext;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    invalidateObjectManager = new InvalidateObjectManagerImpl(Mockito.mock(ServerTransactionManager.class));
    validateSink = new TestSinkAdaptor();
    inValidateSink = new TestSinkAdaptor();
    configurationContext = new TestConfigurationContext();
    invalidateObjectManager.initializeContext(configurationContext);
  }

  public void testBasic() throws Exception {
    invalidateObjectManager.start();

    ClientID cid1 = new ClientID(1);
    ClientID cid2 = new ClientID(2);

    ObjectID mapId1 = new ObjectID(1001);
    ObjectID mapId2 = new ObjectID(1002);
    ObjectID mapId3 = new ObjectID(1003);

    Invalidations invalidations = new Invalidations();
    for (int i = 1; i <= 10; i++) {
      invalidations.add(mapId1, new ObjectID(i));
      invalidations.add(mapId2, new ObjectID(i + 10));
    }

    invalidateObjectManager.invalidateObjectFor(cid1, invalidations);

    Invalidations invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid1);
    Assert.assertTrue(invalidationsGot.equals(invalidations));
    ArrayList list = inValidateSink.getList();
    Assert.assertEquals(1, list.size());
    InvalidateObjectsForClientContext clientContext = (InvalidateObjectsForClientContext) list.get(0);
    Assert.assertEquals(cid1, clientContext.getClientID());
    list.clear();

    invalidations = new Invalidations();
    for (int i = 1; i <= 10; i++) {
      invalidations.add(mapId1, new ObjectID(i));
      invalidations.add(mapId2, new ObjectID(i + 10));
    }
    Invalidations total = new Invalidations();
    total.add(invalidations);

    list = inValidateSink.getList();
    Assert.assertEquals(0, list.size());
    invalidateObjectManager.invalidateObjectFor(cid1, invalidations);
    list = inValidateSink.getList();
    Assert.assertEquals(1, list.size());

    Invalidations invalidations2 = new Invalidations();
    for (int i = 1; i <= 10; i++) {
      invalidations2.add(mapId3, new ObjectID(i + 20));
    }
    total.add(invalidations2);

    invalidateObjectManager.invalidateObjectFor(cid1, invalidations2);
    list = inValidateSink.getList();
    Assert.assertEquals(1, list.size());

    invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid1);
    Assert.assertTrue(invalidationsGot.equals(total));

    list.clear();

    invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid1);
    Assert.assertNull(invalidationsGot);

    Invalidations invalidationsCid1 = new Invalidations();
    Invalidations invalidationsCheckCid1 = new Invalidations();
    Invalidations invalidationsCid2 = new Invalidations();
    Invalidations invalidationsCheckCid2 = new Invalidations();
    for (int i = 1; i <= 10; i++) {
      invalidationsCid1.add(mapId1, new ObjectID(i));
      invalidationsCheckCid1.add(mapId1, new ObjectID(i));
      invalidationsCid2.add(mapId2, new ObjectID(i + 10));
      invalidationsCheckCid2.add(mapId2, new ObjectID(i + 10));
    }

    invalidateObjectManager.invalidateObjectFor(cid1, invalidationsCid1);
    invalidateObjectManager.invalidateObjectFor(cid2, invalidationsCid2);

    list = inValidateSink.getList();
    Assert.assertEquals(2, list.size());
    clientContext = (InvalidateObjectsForClientContext) list.get(0);
    Assert.assertEquals(cid1, clientContext.getClientID());
    clientContext = (InvalidateObjectsForClientContext) list.get(1);
    Assert.assertEquals(cid2, clientContext.getClientID());

    invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid1);
    Assert.assertTrue(invalidationsGot.equals(invalidationsCheckCid1));

    invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid2);
    Assert.assertTrue(invalidationsGot.equals(invalidationsCheckCid2));
  }

  public void testValidations() throws Exception {
    // Valid object ids
    ObjectIDSet validOids = new ObjectIDSet();
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 50; j++) {
        long longOid = 100 * i + j;
        validOids.add(new ObjectID(longOid));
      }
    }

    ClientID[] cids = new ClientID[10];
    for (int i = 0; i < 10; i++) {
      cids[i] = new ClientID(i);
    }

    for (int i = 0; i < 10; i++) {
      Invalidations objectIDsToValidate = new Invalidations();
      for (int j = 0; j < 100; j++) {
        long longOid = 100 * i + j;
        objectIDsToValidate.add(new ObjectID(1000 + i), new ObjectID(longOid));
      }
      invalidateObjectManager.addObjectsToValidateFor(cids[i], objectIDsToValidate);
    }

    invalidateObjectManager.start();

    invalidateObjectManager.validateObjects(validOids);

    ArrayList list = inValidateSink.getList();
    Assert.assertEquals(10, list.size());

    for (int i = 0; i < 10; i++) {
      InvalidateObjectsForClientContext clientContext = (InvalidateObjectsForClientContext) list.get(i);
      ClientID cid = clientContext.getClientID();
      int k = (int) cid.toLong();

      Invalidations invalidationsGot = invalidateObjectManager.getObjectsIDsToInvalidate(cid);
      Invalidations expected = new Invalidations();
      ObjectID mapID = new ObjectID(1000 + k);
      for (int j = 50; j < 100; j++) {
        long longOid = 100 * k + j;
        expected.add(mapID, new ObjectID(longOid));
      }
      Assert.assertEquals(expected, invalidationsGot);
    }
  }

  private class TestConfigurationContext implements ConfigurationContext {

    public TCLogger getLogger(Class clazz) {
      return null;
    }

    public Stage getStage(String name) {
      Stage stage = Mockito.mock(Stage.class);
      if (name.equals(ServerConfigurationContext.INVALIDATE_OBJECTS_STAGE)) {
        Mockito.when(stage.getSink()).thenReturn(inValidateSink);
        return stage;
      } else if (name.equals(ServerConfigurationContext.VALIDATE_OBJECTS_STAGE)) {
        Mockito.when(stage.getSink()).thenReturn(validateSink);
        return stage;
      }
      return null;
    }
  }

  private class TestSinkAdaptor implements Sink {
    private final ArrayList list = new ArrayList();

    public void add(EventContext context) {
      list.add(context);
    }

    public ArrayList getList() {
      return list;
    }

    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    public void addMany(Collection contexts) {
      throw new ImplementMe();
    }

    public void clear() {
      throw new ImplementMe();
    }

    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();
    }

    public int size() {
      throw new ImplementMe();
    }

    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();
    }

    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    public void resetStats() {
      throw new ImplementMe();
    }

  }
}
