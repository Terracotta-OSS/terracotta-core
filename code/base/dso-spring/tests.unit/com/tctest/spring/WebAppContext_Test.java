/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.MethodNameMatcher;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.stub.ReturnStub;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.Singleton;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * Test case for Spring web application context
 * 
 * @author Eugene Kuleshov
 */
public class WebAppContext_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 4;

  public WebAppContext_Test() {
    if (Vm.isIBM()) {
      this.disableAllUntil("2007-10-01");
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return WebAppContextApp.class;
  }
  

  public static class WebAppContextApp extends AbstractTransparentApp {

    private List sharedSingletons = new ArrayList();
    
    
    public WebAppContextApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
        try {
          final Map contextAttributes = new HashMap();

          final String contextName = "beanfactory.xml";
          URL contextUrl = getClass().getResource(contextName);
          assertNotNull("Unable to load context "+contextName, contextUrl);

          Mock servletContextMock = new Mock(ServletContext.class);
          
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("log"))).withAnyArguments().isVoid();
          
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getInitParameter")))
              .with(new IsEqual("locatorFactorySelector")).will(new ReturnStub(null));
          
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getInitParameter")))
              .with(new IsEqual("parentContextKey")).will(new ReturnStub(null));

          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getInitParameter")))
              .with(new IsEqual("contextClass")).will(new ReturnStub(null));
          
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getInitParameter")))
            .with(new IsEqual("contextConfigLocation")).will(new ReturnStub(contextName));
      
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getResource")))
            .with(new IsEqual("/"+contextName)).will(new ReturnStub(contextUrl));
      
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getResourceAsStream")))
            .with(new IsEqual("/"+contextName))
            .will(new CustomStub("getResourceAsStream") {
                public Object invoke(Invocation arg) throws Throwable {
                  return getClass().getResourceAsStream(contextName);
                }
              });
          
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("setAttribute")) {
              public void invoked(Invocation invocation) {
                List params = invocation.parameterValues;
                contextAttributes.put(params.get(0), params.get(1));
              }
            });
        
          servletContextMock.expects(new MethodNameMatcher(new IsEqual("getAttribute")))
            .will(new CustomStub("getAttribute") {
                public Object invoke(Invocation invocation) throws Throwable {
                  return contextAttributes.get(invocation.parameterValues.get(0));
                }
              });
        
          ServletContext servletContext = (ServletContext) servletContextMock.proxy();          
          ContextLoaderListener listener = new ContextLoaderListener();
          listener.contextInitialized(new ServletContextEvent(servletContext));

          WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);

          moveToStageAndWait(1);
          
          Singleton singleton = (Singleton) ctx.getBean("singleton");
          singleton.incrementCounter();

          String applicationId = getApplicationId();
          singleton.setTransientValue(applicationId);
          
          synchronized (sharedSingletons) {
            sharedSingletons.add(singleton);
          }
          
          moveToStageAndWait(2);
          
          assertTrue("Expected more then one object in the collection", sharedSingletons.size()>1);
          
          for (Iterator it = sharedSingletons.iterator(); it.hasNext();) {
            Singleton o = (Singleton) it.next();
            assertTrue("Found non-singleton object", o==singleton);
            assertEquals("Invalid value in shared field "+o, NODE_COUNT, o.getCounter());
            assertEquals("Invalid transient value "+o, applicationId, o.getTransientValue());
          }
          
        } catch (Throwable e) {
          notifyError(e);
           
        }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addRoot("com.tctest.spring.WebAppContext_Test$WebAppContextApp", "sharedSingletons", "sharedSingletons", false);
      config.addAutolock("* com.tctest.spring.WebAppContext_Test$WebAppContextApp.run()", ConfigLockLevel.WRITE);
      
      config.addIncludePattern("org.jmock.core.Invocation");  // needed to make mock definitions happy
      
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory.xml");
      springConfig.addBean("singleton");
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

