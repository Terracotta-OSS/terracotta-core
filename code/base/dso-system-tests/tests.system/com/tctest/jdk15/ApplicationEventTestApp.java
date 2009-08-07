/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import org.apache.commons.lang.StringUtils;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableLogicalInvokeContext;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.tc.object.appevent.NonPortableRootContext;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

public class ApplicationEventTestApp extends AbstractTransparentApp {
  private final int             jmxPort;
  private MBeanServerConnection mbsc;
  private Stage                 stage   = Stage.UNDEFINED;
  private Object                nonPortableRoot;
  private final Root            root    = new Root();
  private final CyclicBarrier   barrier = new CyclicBarrier(2);

  enum Stage {
    UNDEFINED, LOGICAL_INVOKE, FIELD_SET, ROOT
  }

  Object getNonPortableRoot() {
    return nonPortableRoot;
  }

  public ApplicationEventTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    jmxPort = (Integer) cfg.getAttributeObject("jmx-port");
  }

  public void run() {
    setupApplicationEventListener();
    testNonPortableLogicalInvoke();
    testNonPortableFieldSet();
    testNonPortableRoot();
  }

  private void setupApplicationEventListener() {
    JMXConnectorProxy jmx = new JMXConnectorProxy("localhost", jmxPort);
    try {
      mbsc = jmx.getMBeanServerConnection();
      mbsc.addNotificationListener(L2MBeanNames.DSO_APP_EVENTS, new AppEventListener(), null, null);
    } catch (Exception e) {
      throw new RuntimeException("Setting up AppEventListener", e);
    }
  }

  private class AppEventListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      try {
        final Object event = notification.getSource();
        if (event instanceof NonPortableObjectEvent) {
          NonPortableObjectEvent npoe = (NonPortableObjectEvent) event;

          switch (stage) {
            case UNDEFINED:
              notifyError(new RuntimeException("Caught app event at undefined stage"));
              break;
            case LOGICAL_INVOKE:
              try {
                validateNonPortableLogicalInvokeEvent(npoe);
              } catch (Exception e) {
                notifyError(new ErrorContext("Validating NonPortableLogicalInvokeEvent", e));
              }
              break;
            case FIELD_SET:
              try {
                validateNonPortableFieldSetEvent(npoe);
              } catch (Exception e) {
                notifyError(new ErrorContext("Validating NonPortableFieldSetEvent", e));
              }
              break;
            case ROOT:
              try {
                validateNonPortableRootEvent(npoe);
              } catch (Exception e) {
                notifyError(new ErrorContext("Validating NonPortableRootEvent", e));
              }
              break;
          }
        }
      } finally {
        try {
          barrier.await();
        } catch (Exception e) {/**/
        }
      }
    }
  }

  private void notifyWrongContextType(Class expectedContextType, Class realContextType) {
    notifyError(new RuntimeException("Expected '" + expectedContextType.getName() + "' [was '"
                                     + realContextType.getName() + "'"));
  }

  private void testNonPortableLogicalInvoke() {
    stage = Stage.LOGICAL_INVOKE;
    try {
      synchronized (root) {
        root.map.put("threadKey", new Thread());
        notifyError(new RuntimeException("Shouldn't be able to add a thread to a shared map."));
      }
    } catch (Throwable t) {/**/
    }
    waitAndResetBarrier();
  }

  private void validateNonPortableLogicalInvokeEvent(NonPortableObjectEvent event) {
    ApplicationEventContext cntx = event.getApplicationEventContext();
    if (!(cntx instanceof NonPortableLogicalInvokeContext)) {
      notifyWrongContextType(NonPortableLogicalInvokeContext.class, cntx.getClass());
    }
    NonPortableLogicalInvokeContext logicalInvokeContext = (NonPortableLogicalInvokeContext) cntx;
    Assert.assertEquals(logicalInvokeContext.getTargetClassName(), root.map.getClass().getName());
    Assert.assertEquals(logicalInvokeContext.getLogicalMethod(), "put(Object,Object)");
    Assert.assertEquals(logicalInvokeContext.getParameterIndex(), 1);
    String threadTypeName = Thread.class.getName();
    NonPortableObjectState npos = findObjectStateByTypeName(cntx, threadTypeName);
    Assert.assertNotNull("NonPortableObjectState for type '" + threadTypeName + "'", npos);
  }

  private void testNonPortableFieldSet() {
    stage = Stage.FIELD_SET;
    try {
      synchronized (root) {
        root.struct.field = new Thread();
        notifyError(new RuntimeException("Shouldn't be able to set a thread as a field of a shared object."));
      }
    } catch (Throwable t) {/**/
    }
    waitAndResetBarrier();
  }

  private void validateNonPortableFieldSetEvent(NonPortableObjectEvent event) {
    ApplicationEventContext cntx = event.getApplicationEventContext();
    if (!(cntx instanceof NonPortableFieldSetContext)) {
      notifyWrongContextType(NonPortableFieldSetContext.class, cntx.getClass());
    }
    NonPortableFieldSetContext fieldSetContext = (NonPortableFieldSetContext) cntx;
    String targetClassName = root.struct.getClass().getName();
    String fieldName = targetClassName + ".field";
    Assert.assertEquals(fieldSetContext.getFieldName(), fieldName);
    Assert.assertEquals(fieldSetContext.getTargetClassName(), targetClassName);
    NonPortableObjectState npos = findObjectStateByFieldName(cntx, fieldName);
    Assert.assertNotNull("NonPortableObjectState for field '" + fieldName + "'", npos);
  }

  private void testNonPortableRoot() {
    stage = Stage.ROOT;
    try {
      nonPortableRoot = new NonPortableRoot();
      notifyError(new RuntimeException("Shouldn't be able to create a root that is a thread."));
    } catch (Throwable t) {/**/
    }
    waitAndResetBarrier();
  }

  private void validateNonPortableRootEvent(NonPortableObjectEvent event) {
    ApplicationEventContext cntx = event.getApplicationEventContext();
    if (!(cntx instanceof NonPortableRootContext)) {
      notifyWrongContextType(NonPortableRootContext.class, cntx.getClass());
    }
    NonPortableRootContext rootContext = (NonPortableRootContext) cntx;
    Assert.assertEquals(rootContext.getFieldName(), "nonPortableRoot");
    Assert.assertEquals(rootContext.getTargetClassName(), NonPortableRoot.class.getName());
    NonPortableObjectState npos = findObjectStateByFieldName(cntx, "nonPortableRoot");
    Assert.assertNotNull("NonPortableObjectState for field 'nonPortableRoot'", npos);
  }

  private NonPortableObjectState findObjectStateByFieldName(ApplicationEventContext cntx, String fieldName) {
    TreeModel treeModel = cntx.getTreeModel();
    if (treeModel == null) return null;
    return findObjectStateByFieldName((DefaultMutableTreeNode) treeModel.getRoot(), fieldName);
  }

  private NonPortableObjectState findObjectStateByFieldName(DefaultMutableTreeNode node, String fieldName) {
    Object userObject = node.getUserObject();
    if (userObject instanceof NonPortableObjectState) {
      NonPortableObjectState npos = (NonPortableObjectState) userObject;
      if (StringUtils.equals(fieldName, npos.getFieldName())) return npos;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
      NonPortableObjectState npos = findObjectStateByFieldName(child, fieldName);
      if (npos != null) return npos;
    }
    return null;
  }

  private NonPortableObjectState findObjectStateByTypeName(ApplicationEventContext cntx, String typeName) {
    TreeModel treeModel = cntx.getTreeModel();
    if (treeModel == null) return null;
    return findObjectStateByTypeName((DefaultMutableTreeNode) treeModel.getRoot(), typeName);
  }

  private NonPortableObjectState findObjectStateByTypeName(DefaultMutableTreeNode node, String typeName) {
    Object userObject = node.getUserObject();
    if (userObject instanceof NonPortableObjectState) {
      NonPortableObjectState npos = (NonPortableObjectState) userObject;
      if (StringUtils.equals(typeName, npos.getTypeName())) return npos;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
      NonPortableObjectState npos = findObjectStateByTypeName(child, typeName);
      if (npos != null) return npos;
    }
    return null;
  }

  private void waitAndResetBarrier() {
    try {
      barrier.await();
    } catch (Exception e) {/**/
    }
    barrier.reset();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ApplicationEventTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("nonPortableRoot", "nonPortableRoot");
  }

  private static class NonPortableRoot extends Thread {/**/
  }

  private static class Struct {
    @SuppressWarnings("unused")
    Object field;
  }

  private static class Root {
    public HashMap map    = new HashMap();
    public Struct  struct = new Struct();

    public Root() {
      super();
    }
  }
}
