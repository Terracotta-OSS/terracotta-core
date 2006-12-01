/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.ProductInfo;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;

/**
 * This is shown in the AboutDialog.
 */

public class ConfiguratorInfoPanel extends XContainer {
  private XTextArea m_systemInformationTextArea;
  private Label     m_copyrightLabel;

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_systemInformationTextArea = (XTextArea)findComponent("SystemInformationTextArea");
    m_copyrightLabel            = (Label)findComponent("CopyrightLabel");
  }
  
  public void init(String title, ProductInfo productInfo) {
    String version = productInfo.getVersion();
    String newLine = System.getProperty("line.separator");
    String osInfo  = System.getProperty("os.name") + " (" +
                     System.getProperty("os.version") + "/" +
                     System.getProperty("os.arch") + ")";
    String javaVersion = "Java " + System.getProperty("java.version") + ", " +
                                   System.getProperty("java.vendor");
    String javaHomeDir = System.getProperty("java.home");
    String javaVMInfo =  System.getProperty("java.vm.name") + ", " +
                         System.getProperty("java.vm.version") + " [" +
                         Runtime.getRuntime().maxMemory()/(1024*1024) + " MB]";

    m_systemInformationTextArea.setText(
      title + " " + version + newLine +
      osInfo + newLine +
      javaVersion + newLine +
      javaHomeDir + newLine +
      javaVMInfo);

    m_copyrightLabel.setText(productInfo.getCopyright());
  }
}
