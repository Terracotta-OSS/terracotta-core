/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.SettableConfigItem;
import com.tc.object.config.schema.AutoLock;
import com.tc.object.config.schema.Lock;
import com.tc.object.config.schema.LockLevel;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;

import java.io.File;

/**
 * Unit test for {@link TestTVSConfigurationSetupManagerFactory}. Because that class builds up a whole config system,
 * this test actually stresses a large swath of the configuration system.
 */
public class TestTVSConfigurationSetupManagerFactoryTest extends TCTestCase {

  private TestTVSConfigurationSetupManagerFactory factory;
  private L2TVSConfigurationSetupManager          l2Manager;
  private L1TVSConfigurationSetupManager          l1Manager;

  public void setUp() throws Exception {
    this.factory = new TestTVSConfigurationSetupManagerFactory(
                                                               TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                               null, new FatalIllegalConfigurationChangeHandler());

    ((SettableConfigItem) this.factory.l2CommonConfig().logsPath()).setValue(getTempFile("l2-logs").toString());
    ((SettableConfigItem) this.factory.l1CommonConfig().logsPath()).setValue(getTempFile("l1-logs").toString());

    this.l2Manager = this.factory.createL2TVSConfigurationSetupManager(null);
    this.l1Manager = this.factory.createL1TVSConfigurationSetupManager();
  }

  public void testSettingValues() throws Exception {
    // A string array value
    ((SettableConfigItem) factory.dsoApplicationConfig().transientFields()).setValue(new String[] { "Foo.foo",
        "Bar.bar" });

    // Hit the remaining top-level config objects
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionInterval()).setValue(142);
    ((SettableConfigItem) factory.l1CommonConfig().logsPath()).setValue("whatever");
    ((SettableConfigItem) factory.l2CommonConfig().dataPath()).setValue("marph");

    // A complex value (locks)
    ((SettableConfigItem) factory.dsoApplicationConfig().locks()).setValue(createLocks(new Lock[] {
        new AutoLock("Foo.foo(..)", LockLevel.CONCURRENT),
        new com.tc.object.config.schema.NamedLock("bar", "Baz.baz(..)", LockLevel.READ) }));

    // A sub-config object
    ((SettableConfigItem) factory.l1DSOConfig().instrumentationLoggingOptions().logDistributedMethods()).setValue(true);

    this.factory.activateConfigurationChange();

    System.err.println(this.l2Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));
    System.err.println(this.l2Manager.systemConfig());
    System.err.println(this.l1Manager.dsoL1Config());

    assertEqualsOrdered(new String[] { "Foo.foo", "Bar.bar" }, this.l2Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME).transientFields()
        .getObject());
    assertEquals(142, this.l2Manager.dsoL2Config().garbageCollectionInterval().getInt());
    assertEquals(new File("whatever"), this.l1Manager.commonL1Config().logsPath().getFile());
    assertEquals(new File("marph"), this.l2Manager.commonl2Config().dataPath().getFile());
    assertEqualsUnordered(new Lock[] { new AutoLock("Foo.foo(..)", LockLevel.CONCURRENT),
        new com.tc.object.config.schema.NamedLock("bar", "Baz.baz(..)", LockLevel.READ) }, this.l2Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME).locks().getObject());
    assertTrue(this.l1Manager.dsoL1Config().instrumentationLoggingOptions().logDistributedMethods().getBoolean());
  }

  private com.terracottatech.config.LockLevel.Enum level(LockLevel in) {
    if (in.equals(LockLevel.CONCURRENT)) return com.terracottatech.config.LockLevel.CONCURRENT;
    if (in.equals(LockLevel.READ)) return com.terracottatech.config.LockLevel.READ;
    if (in.equals(LockLevel.WRITE)) return com.terracottatech.config.LockLevel.WRITE;
    throw Assert.failure("Unknown lock level " + in);
  }

  private Locks createLocks(Lock[] locks) {
    Locks out = Locks.Factory.newInstance();
    for (int i = 0; i < locks.length; ++i) {
      if (locks[i].isAutoLock()) {
        Autolock lock = out.addNewAutolock();
        lock.setLockLevel(level(locks[i].lockLevel()));
        lock.setMethodExpression(locks[i].methodExpression());
      } else {
        NamedLock lock = out.addNewNamedLock();
        lock.setLockLevel(level(locks[i].lockLevel()));
        lock.setMethodExpression(locks[i].methodExpression());
        lock.setLockName(locks[i].lockName());
      }
    }
    return out;
  }

}
