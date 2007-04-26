
package com.tctest.wicket;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.util.tester.ITestPageSource;
import org.apache.wicket.util.tester.WicketTester;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;

public class DistributedWicketTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    setTimeoutThreshold(600000L);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return DistributedWicketTestApp.class;
  }

  
  public static void main(String[] args) {
    new DistributedWicketTestApp().run();
  }
  
  public static class DistributedWicketTestApp extends AbstractTransparentApp {

    public DistributedWicketTestApp() {
    }
    
    public DistributedWicketTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      // config.addNewModule("clustered-wicket-1.3", "1.0.0");
    }
    
    public void run() {
      WicketTester tester = new WicketTester();
      
      tester.startPage(Page1.class);
      tester.assertRenderedPage(Page1.class);
      tester.assertLabel("message1", "Hello!");

      // click test
      tester.startPage(Page1.class);
      tester.clickLink("toPage2");
      tester.assertRenderedPage(Page2.class);
      tester.assertLabel("message2", "Hi!");
      
      tester.startPage(new ITestPageSource() {
        private static final long serialVersionUID = 1L;
        public Page getTestPage() {
          return new Page2("mock message");
        }
      });
      tester.assertRenderedPage(Page2.class);
      tester.assertLabel("message2", "mock message");
      tester.assertInfoMessages(new String[] { "Hi again!" });
    }
    
  }

  
  public static class Page1 extends WebPage {
    private static final long serialVersionUID = 1L;

    public Page1() {
      add(new Label("message1", "Hello!"));
      add(new Link("toPage2") {
        private static final long serialVersionUID = 1L;
        public void onClick() {
          setResponsePage(new Page2("Hi!"));
        }
      });
    }

  }

  
  public static class Page2 extends WebPage {
    private static final long serialVersionUID = 1L;

    public Page2(String message) {
      add(new Label("message2", message));
      info("Hi again!");
    }
  }  
  
}

