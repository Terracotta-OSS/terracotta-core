/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import org.terracotta.modules.configuration.Presentation;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClusterModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.event.EventListenerList;

public class FeatureNode extends ComponentNode implements PropertyChangeListener {
  protected final Feature             feature;
  protected final IAdminClientContext adminClientContext;
  protected final IClusterModel       clusterModel;
  protected final EventListenerList   listenerList;

  protected FeaturePanel              featurePanel;
  protected ClientsNode               clientsNode;

  public FeatureNode(Feature feature, IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(feature.getDisplayName());

    this.feature = feature;
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.featurePanel = new FeaturePanel(feature, this, adminClientContext, clusterModel);
    this.listenerList = new EventListenerList();

    this.featurePanel.addPropertyChangeListener(this);

    setComponent(this.featurePanel);
    setName(feature.getDisplayName());
  }

  public boolean isPresentationReady() {
    Presentation p = featurePanel.getPresentation();
    return p != null && p.isReady();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (Presentation.PROP_PRESENTATION_READY.equals(prop)) {
      firePresentationReady(((Boolean) evt.getNewValue()).booleanValue());
    }
  }

  public void addPresentationListener(PresentationListener listener) {
    listenerList.add(PresentationListener.class, listener);
  }

  public void removePresentationListener(PresentationListener listener) {
    listenerList.remove(PresentationListener.class, listener);
  }

  private void firePresentationReady(boolean ready) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == PresentationListener.class) {
        ((PresentationListener) listeners[i + 1]).presentationReady(ready);
      }
    }
  }

  private volatile boolean tearDown = true;

  public void setTearDown(boolean tearDown) {
    this.tearDown = tearDown;
  }

  @Override
  public void tearDown() {
    if (!tearDown) { return; }

    featurePanel.removePropertyChangeListener(this);
    featurePanel.tearDown();

    super.tearDown();
  }
}
