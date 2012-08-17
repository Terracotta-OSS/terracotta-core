/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.terracottatech.config.L1ReconnectPropertiesDocument;
import com.terracottatech.config.L1ReconnectPropertiesDocument.L1ReconnectProperties;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

public class L1ReconnectDisabledTest extends AbstractToolkitTestBase {

  public L1ReconnectDisabledTest(TestConfig testConfig) {
    super(testConfig, App.class);
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED + "=false");
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS + "="
                                                      + App.L1_RECONNECT_TIMEOUT);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(L1ReconnectPropertiesDocument.class));
    extraJars.add(TestBaseUtil.jarFor(SchemaTypeSystemImpl.class));
    return extraJars;
  }

  public static class App extends ClientBase {
    private static final int L1_RECONNECT_TIMEOUT = 5678;

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      URL url = new URL("http", "localhost", getTestControlMbean().getGroupsData()[0].getDsoPort(0),
                        "/l1reconnectproperties");
      InputStream in = url.openStream();

      L1ReconnectPropertiesDocument l1ReconnectPropFromL2Doc = L1ReconnectPropertiesDocument.Factory.parse(in);
      L1ReconnectProperties l1ReconnectPropFromL2 = l1ReconnectPropFromL2Doc.getL1ReconnectProperties();

      Assert.assertFalse(l1ReconnectPropFromL2.getL1ReconnectEnabled());
      Assert.assertEquals(L1_RECONNECT_TIMEOUT, l1ReconnectPropFromL2.getL1ReconnectTimeout().intValue());
    }

  }
}
