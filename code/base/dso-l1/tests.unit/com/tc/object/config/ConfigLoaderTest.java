/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
    
    if(application!=null) {
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
        loader.loadSpringConfig(application.getSpring());
      } catch (XmlValueOutOfRangeException e) {
        fail(e.getMessage());
      }
      
      assertTrue("Parsing errors: " + errors.toString(), errors.isEmpty());
    }
  }

  public String getName() {
    return super.getName() + " : " + configName;
  }

  
  public static TestSuite suite() {
    TestSuite suite = new TestSuite(ConfigLoaderTest.class.getName());

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
    
    suite.addTest(new ConfigLoaderTest("anothersingleton-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("aop-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("app-ctx-matching-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("appctxdef-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("customscoped-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("empty-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("event-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("hibernate-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("init2-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("interceptor-via-postprocessor-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("lifecycle-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("multibeandef-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("multicontext-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("multifile-context-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("parent-child-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("proxiedbean-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("redeployment-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("referenceandreplication-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("scopedbeans-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("sellitem-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("sessionscoped-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("sharedlock-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("singleton-parent-child-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("thread-coordination-tc-config.xml"));
    suite.addTest(new ConfigLoaderTest("webflow-tc-config.xml"));

    return suite;
  }
  
}

