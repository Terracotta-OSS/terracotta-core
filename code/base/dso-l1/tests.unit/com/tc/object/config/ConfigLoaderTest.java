/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.terracottatech.config.Application;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

public class ConfigLoaderTest extends TestCase {

  public void testEmptyConfig() throws Exception {
    test("empty-tc-config.xml");
    test("tc-config-dso.xml");
    test("tc-config-chatter.xml");
    test("tc-config-coordination.xml");
    test("tc-config-inventory.xml");
    
    test("tc-config-jtable.xml");
    test("tc-config-l2.xml");
    test("tc-config-scoordination.xml");
    test("tc-config-sevents.xml");
    test("tc-config-sharededitor.xml");
    test("tc-config-sharedqueue.xml");
    test("tc-config-sjmx.xml");
    test("tc-config-swebflow.xml");    
    
    test("anothersingleton-tc-config.xml");
    test("aop-tc-config.xml");
    test("app-ctx-matching-tc-config.xml");
    test("appctxdef-tc-config.xml");
    test("customscoped-tc-config.xml");
    test("empty-tc-config.xml");
    test("event-tc-config.xml");
    test("hibernate-tc-config.xml");
    test("init2-tc-config.xml");
    test("interceptor-via-postprocessor-tc-config.xml");
    test("lifecycle-tc-config.xml");
    test("multibeandef-tc-config.xml");
    test("multicontext-tc-config.xml");
    test("multifile-context-tc-config.xml");
    test("parent-child-tc-config.xml");
    test("proxiedbean-tc-config.xml");
    test("redeployment-tc-config.xml");
    test("referenceandreplication-tc-config.xml");
    test("scopedbeans-tc-config.xml");
    test("sellitem-tc-config.xml");
    test("sessionscoped-tc-config.xml");
    test("sharedlock-tc-config.xml");
    test("singleton-parent-child-tc-config.xml");
    test("thread-coordination-tc-config.xml");
    test("webflow-tc-config.xml");
  }


  private void test(String name) throws XmlException, IOException, ConfigurationSetupException {
    InputStream is = getClass().getResourceAsStream(name);
    
    // Application app = Application.Factory.parse(is);
    TcConfigDocument tcConfigDocument = TcConfigDocument.Factory.parse(is);
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
      loader.loadDsoConfig(application.getDso());
      loader.loadSpringConfig(application.getSpring());
    }
  }

}

