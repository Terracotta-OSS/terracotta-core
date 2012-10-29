/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.properties.TCPropertiesConsts;

import java.lang.reflect.Method;

import junit.framework.Assert;

public class TcPropertiesOverWriteTestApp extends ClientBase {
  private static final String MANAGER_UTIL_CLASS_NAME                        = "com.tc.object.bytecode.ManagerUtil";
  private static final String MANAGER_UTIL_getTCProperties_METHOD            = "getTCProperties";
  private static final String TCPROPERTIES_getProperty_METHOD                = "getProperty";

  public static String        L1_CACHEMANAGER_ENABLED_VALUE                  = "true";
  public static String        L1_LOGGING_MAX_LOGFILE_SIZE_VALUE              = "1234";
  public static String        L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE = "5678";
  public static String        L1_CACHEMANAGER_LEASTCOUNT_VALUE               = "15";

  public TcPropertiesOverWriteTestApp(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new TcPropertiesOverWriteTestApp(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    Assert.assertEquals(getTcPropertyValueFor(TCPropertiesConsts.L1_CACHEMANAGER_ENABLED.toUpperCase()),
                        L1_CACHEMANAGER_ENABLED_VALUE);
    Assert.assertEquals(getTcPropertyValueFor(TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE),
                        L1_LOGGING_MAX_LOGFILE_SIZE_VALUE);
    Assert
        .assertEquals(getTcPropertyValueFor(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES.replace("e",
                                                                                                                "E")),
                      L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE);
    Assert.assertEquals(getTcPropertyValueFor(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT.toLowerCase()),
                        L1_CACHEMANAGER_LEASTCOUNT_VALUE);
  }

  // work around for ManagerUtil
  public String getTcPropertyValueFor(String key) {
    try {
      ClassLoader cl = getClusteringToolkit().getMap("testMap", null, null).getClass().getClassLoader();
      Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
      Method getTCPropertiesMethod = managerUtil.getDeclaredMethod(MANAGER_UTIL_getTCProperties_METHOD);
      getTCPropertiesMethod.setAccessible(true);
      Object tcProps = getTCPropertiesMethod.invoke(null);

      return (String) tcProps.getClass().getMethod(TCPROPERTIES_getProperty_METHOD, String.class).invoke(tcProps, key);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
