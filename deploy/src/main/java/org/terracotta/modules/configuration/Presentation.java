/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.configuration;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClusterModel;

import javax.swing.Icon;
import javax.swing.JPanel;

public abstract class Presentation extends JPanel {
  public static final String PROP_PRESENTATION_READY = "presentationReady";

  public abstract void setup(ApplicationContext appContext, IClusterModel clusterModel);

  public abstract boolean isReady();

  public Icon getIcon() {
    return null;
  }

  public abstract void tearDown();

  public abstract void startup();
}
