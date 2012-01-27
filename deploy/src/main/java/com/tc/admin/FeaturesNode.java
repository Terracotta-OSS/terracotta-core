/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;

import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.SyncHTMLEditorKit;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.dso.DSOHelper;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.management.beans.TIMByteProviderMBean;

import java.awt.Component;
import java.awt.Cursor;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class FeaturesNode extends ComponentNode implements NotificationListener, HyperlinkListener {
  protected final IAdminClientContext         adminClientContext;
  protected final IClusterModel               clusterModel;
  protected final ClusterNode                 clusterNode;
  protected final ClusterListener             clusterListener;

  protected XScrollPane                       myApplicationPanel;
  protected ClientsNode                       clientsNode;
  protected Map<String, Feature>              activeFeatureMap;
  protected Map<Feature, FeatureNode>         nodeMap;

  protected static final Map<String, Feature> allFeaturesMap = new LinkedHashMap<String, Feature>();

  public FeaturesNode(ClusterNode clusterNode, IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("cluster.features"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.clusterNode = clusterNode;
    this.clusterListener = new ClusterListener(clusterModel);
    this.activeFeatureMap = new LinkedHashMap<String, Feature>();
    this.nodeMap = new LinkedHashMap<Feature, FeatureNode>();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.getActiveCoordinator() != null) {
      init();
    }

    setIcon(DSOHelper.getHelper().getFeaturesIcon());
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (newActive != null) {
        init();
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      adminClientContext.log(e);
    }
  }

  private void addMBeanServerDelegateListener() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    try {
      ObjectName on = new ObjectName("JMImplementation:type=MBeanServerDelegate");
      activeCoord.addNotificationListener(on, this);
    } catch (Exception e) {
      /**/
    }
  }

  private void removeMBeanServerDelegateListener() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        ObjectName on = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        activeCoord.addNotificationListener(on, this);
      } catch (Exception e) {
        /**/
      }
    }
  }

  public void init() {
    addMBeanServerDelegateListener();

    Set<ObjectName> s;
    try {
      ObjectName on = new ObjectName("org.terracotta:type=Loader,*");
      s = clusterModel.getActiveCoordinator().queryNames(on, null);
    } catch (Exception e) {
      s = Collections.emptySet();
    }

    Iterator<ObjectName> iter = s.iterator();
    while (iter.hasNext()) {
      testRegisterFeature(iter.next());
    }
    ensureFeatureNodes();
  }

  private void ensureFeatureNodes() {
    Iterator<Map.Entry<String, Feature>> featureIter = activeFeatureMap.entrySet().iterator();
    while (featureIter.hasNext()) {
      Map.Entry<String, Feature> entry = featureIter.next();
      Feature feature = entry.getValue();
      if (!nodeMap.containsKey(feature)) {
        FeatureNode featureNode = newFeatureNode(feature);
        handlePresentationReady(featureNode);
      }
    }
  }

  private FeatureNode newFeatureNode(Feature feature) {
    FeatureNode node = new FeatureNode(feature, adminClientContext, clusterModel);
    nodeMap.put(feature, node);
    node.addPresentationListener(new PresentationListenerImpl(node));
    return node;
  }

  private class PresentationListenerImpl implements PresentationListener {
    private final FeatureNode featureNode;

    PresentationListenerImpl(FeatureNode featureNode) {
      this.featureNode = featureNode;
    }

    public void presentationReady(boolean ready) {
      handlePresentationReady(featureNode);
    }
  }

  private void testAddToParent() {
    if (getParent() == null) {
      clusterNode.insertChild(this, 0);
    }
  }

  private void testRemoveFromParent() {
    if (getChildCount() == 0) {
      removeFromClusterNode();
    }
  }

  private void handlePresentationReady(FeatureNode featureNode) {
    if (featureNode.isPresentationReady()) {
      testAddToParent();
      int i = 0;
      for (Enumeration<FeatureNode> ce = children(); ce.hasMoreElements();) {
        if (featureNode.getLabel().compareTo(ce.nextElement().getLabel()) < 0) {
          break;
        }
        i++;
      }
      insertChild(featureNode, i);
      adminClientContext.getAdminClientController().expand(this);
      adminClientContext.getAdminClientController().activeFeatureAdded(featureNode.getName());
    } else if (isNodeChild(featureNode)) {
      removeFeatureNode(featureNode);
      adminClientContext.getAdminClientController().activeFeatureRemoved(featureNode.getName());
      testRemoveFromParent();
    }
  }

  /**
   * We don't want the featureNode to get torn down by the tree because the feature loader is still present, it's just
   * that the presentation said it wasn't ready.
   */
  private void removeFeatureNode(FeatureNode featureNode) {
    featureNode.setTearDown(false);
    removeChild(featureNode);
    featureNode.setTearDown(true);
  }

  protected boolean testRegisterFeature(ObjectName on) {
    if (StringUtils.equals("org.terracotta", on.getDomain()) && StringUtils.equals("Loader", on.getKeyProperty("type"))) {
      String symbolicName = on.getKeyProperty("feature");
      String name = on.getKeyProperty("name");
      Feature feature;
      synchronized (allFeaturesMap) {
        feature = allFeaturesMap.get(symbolicName);
        if (feature == null) {
          feature = new Feature(symbolicName, name);
          allFeaturesMap.put(symbolicName, feature);
        }
      }
      IServer activeCoord = clusterModel.getActiveCoordinator();
      TIMByteProviderMBean byteProvider = activeCoord.getMBeanProxy(on, TIMByteProviderMBean.class);
      feature.getFeatureClassLoader().addTIMByteProvider(on, byteProvider);
      if (!activeFeatureMap.containsKey(symbolicName)) {
        activeFeatureMap.put(symbolicName, feature);
        return true;
      }
    }
    return false;
  }

  protected boolean testUnregisterFeature(ObjectName on) {
    if (StringUtils.equals("org.terracotta", on.getDomain()) && StringUtils.equals("Loader", on.getKeyProperty("type"))) {
      String symbolicName = on.getKeyProperty("feature");
      Feature feature = activeFeatureMap.get(symbolicName);
      if (feature != null) {
        feature.getFeatureClassLoader().removeTIMByteProvider(on);
        if (feature.getFeatureClassLoader().getTIMByteProviderCount() == 0) {
          tearDownFeature(feature);
          activeFeatureMap.remove(feature.getSymbolicName());
        }
        return true;
      }
    }
    return false;
  }

  private void tearDownFeature(Feature feature) {
    FeatureNode featureNode = nodeMap.remove(feature);
    if (featureNode != null) {
      if (isNodeChild(featureNode)) {
        removeChild(featureNode);
      }
      adminClientContext.getAdminClientController().activeFeatureRemoved(featureNode.getName());
      featureNode.tearDown();
    }
    if (getParent() != null && getChildCount() == 0) {
      removeFromClusterNode();
    }
  }

  private void removeFromClusterNode() {
    setTearDown(false);
    clusterNode.removeChild(this);
    setTearDown(true);
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();
    if (notification instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (testRegisterFeature(mbsn.getMBeanName())) {
              ensureFeatureNodes();
            }
          }
        });
      } else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            testUnregisterFeature(mbsn.getMBeanName());
          }
        });
      }
    }
  }

  @Override
  public Component getComponent() {
    if (myApplicationPanel == null) {
      XTextPane textPane = new XTextPane();
      textPane.setEditorKit(new SyncHTMLEditorKit());
      myApplicationPanel = new XScrollPane(textPane);
      try {
        textPane.setPage(getClass().getResource("MyApplication.html"));
      } catch (Exception e) {
        adminClientContext.log(e);
      }
      textPane.setEditable(false);
      textPane.addHyperlinkListener(this);
    }
    return myApplicationPanel;
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    XTextPane textPane = (XTextPane) e.getSource();
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

    if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet anchor = (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
      String url = (String) anchor.getAttribute(HTML.Attribute.HREF);
      BrowserLauncher.openURL(url);
    }
  }

  private volatile boolean tearDown = false;

  public void setTearDown(boolean tearDown) {
    this.tearDown = tearDown;
  }

  @Override
  public void tearDown() {
    if (!tearDown) { return; }

    removeMBeanServerDelegateListener();

    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    synchronized (activeFeatureMap) {
      for (Feature feature : activeFeatureMap.values()) {
        tearDownFeature(feature);
      }
      activeFeatureMap.clear();
    }

    super.tearDown();
  }
}
