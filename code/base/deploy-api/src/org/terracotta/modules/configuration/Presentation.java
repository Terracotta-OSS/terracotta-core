/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.configuration;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClusterModel;

import javax.swing.JPanel;

public abstract class Presentation extends JPanel {
  public abstract void setup(ApplicationContext appContext, IClusterModel clusterModel);

  public abstract void tearDown();
}
