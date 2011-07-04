/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException;

import com.tc.config.Loader;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.terracottatech.config.Application;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ConfigLoaderTest extends TestCase {

  private final String configName;

  public ConfigLoaderTest(String configName) {
    super("test");
    this.configName = configName;
  }

  public void test() throws XmlException, IOException, ConfigurationSetupException {
    ArrayList errors = new ArrayList();

    TcConfigDocument tcConfigDocument = new Loader().parse(getClass().getResourceAsStream(configName), //
                                                           new XmlOptions().setLoadLineNumbers().setValidateOnSet());
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    Application application = tcConfig.getApplication();

    if (application != null) {
      TCLogger logger = new NullTCLogger();

      DSOClientConfigHelper config = (DSOClientConfigHelper) Proxy
          .newProxyInstance(getClass().getClassLoader(), new Class[] { DSOClientConfigHelper.class },
                            new InvocationHandler() {
                              public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                return null;
                              }
                            });

      ConfigLoader loader = new ConfigLoader(config, logger);
      try {
        loader.loadDsoConfig(application.getDso());
      } catch (XmlValueOutOfRangeException e) {
        fail(e.getMessage());
      }

      assertTrue("Parsing errors: " + errors.toString(), errors.isEmpty());
    }
  }

  @Override
  public String getName() {
    return super.getName() + " : " + configName;
  }

  public static TestSuite suite() {
    TestSuite suite = new TestSuite(ConfigLoaderTest.class.getName());

    // this to make sure backward compatibility with terracotta-4.xsd
    // update tc-config-reference.xml if this test fail or there's change to
    // terracotta-4.xsd (upgrade version, etc)
    suite.addTest(new ConfigLoaderTest("tc-config-reference.xml"));

    suite.addTest(new ConfigLoaderTest("empty-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-dso.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-chatter.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-coordination.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-inventory.xml"));

    suite.addTest(new ConfigLoaderTest("tc-config-jtable.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-l2.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-scoordination.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-sevents.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-sharededitor.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-sharedqueue.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-sjmx.xml"));
    suite.addTest(new ConfigLoaderTest("tc-config-swebflow.xml"));

    return suite;
  }

}
