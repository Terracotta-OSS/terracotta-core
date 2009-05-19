/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.management.beans.TIMByteProviderMBean;

import java.awt.Component;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class FeaturesNode extends ComponentNode implements NotificationListener {
  protected IAdminClientContext       adminClientContext;
  protected IClusterModel             clusterModel;
  protected ClusterListener           clusterListener;
  protected FeaturesPanel             featuresPanel;
  protected ClientsNode               clientsNode;
  protected Map<String, Feature>      featureMap;
  protected Map<Feature, FeatureNode> nodeMap;

  public FeaturesNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("cluster.features"));
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.clusterListener = new ClusterListener(clusterModel);
    featureMap = new LinkedHashMap<String, Feature>();
    nodeMap = new LinkedHashMap<Feature, FeatureNode>();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.getActiveCoordinator() != null) {
      init();
    }
  }

  protected synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (newActive != null) {
        init();
      }
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
      s = clusterModel.getActiveCoordinator().queryNames(null, null);
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
    Iterator<Map.Entry<String, Feature>> featureIter = featureMap.entrySet().iterator();
    while (featureIter.hasNext()) {
      Map.Entry<String, Feature> entry = featureIter.next();
      Feature feature = entry.getValue();
      if (!nodeMap.containsKey(feature)) {
        addChild(newFeatureNode(feature));
        if (featuresPanel != null) {
          featuresPanel.add(feature);
        }
      }
    }
  }

  private FeatureNode newFeatureNode(Feature feature) {
    FeatureNode node = new FeatureNode(feature, adminClientContext, clusterModel);
    nodeMap.put(feature, node);
    return node;
  }

  protected boolean testRegisterFeature(ObjectName on) {
    if (StringUtils.equals("org.terracotta", on.getDomain()) && StringUtils.equals("Loader", on.getKeyProperty("type"))) {
      String symbolicName = on.getKeyProperty("feature");
      String name = on.getKeyProperty("name");
      Feature feature = featureMap.get(symbolicName);
      if (feature == null) {
        feature = new Feature(symbolicName, name);
        featureMap.put(symbolicName, feature);
      }
      TIMByteProviderMBean byteProvider = clusterModel.getActiveCoordinator().getMBeanProxy(on,
                                                                                            TIMByteProviderMBean.class);
      feature.addTIMByteProvider(on, byteProvider);
      if (featuresPanel != null) {
        featuresPanel.add(feature);
      }
      return true;
    }
    return false;
  }

  protected boolean testUnregisterFeature(ObjectName on) {
    if (StringUtils.equals("org.terracotta", on.getDomain()) && StringUtils.equals("Loader", on.getKeyProperty("type"))) {
      String symbolicName = on.getKeyProperty("feature");
      Feature feature = featureMap.get(symbolicName);
      if (feature != null) {
        feature.removeTIMByteProvider(on);
        if (feature.getTIMByteProviderCount() == 0) {
          tearDownFeature(feature);
        }
        return true;
      }
    }
    return false;
  }

  private void tearDownFeature(Feature feature) {
    if (featuresPanel != null) {
      featuresPanel.remove(feature);
    }
    FeatureNode node = nodeMap.get(feature);
    if (node != null) {
      removeChild(node);
      node.tearDown();
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();
    if (notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (testRegisterFeature(mbsn.getMBeanName())) {
          ensureFeatureNodes();
        }
      } else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        testUnregisterFeature(mbsn.getMBeanName());
      }
    }
  }

  @Override
  public Component getComponent() {
    if (featuresPanel == null) {
      featuresPanel = new FeaturesPanel(adminClientContext, clusterModel);
      featuresPanel.init(featureMap);
    }
    return featuresPanel;
  }

  @Override
  public void tearDown() {
    removeMBeanServerDelegateListener();

    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      featureMap.clear();
      featureMap = null;
      nodeMap.clear();
      nodeMap = null;
    }

    super.tearDown();
  }
}
