/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.ApplicationManager;
import org.dijon.Button;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.DictionaryResource;
import org.dijon.Label;

import com.tc.util.ProductInfo;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

public class AboutDialog extends Dialog implements ActionListener {
  public AboutDialog(Frame parent) {
    super(parent, true);

    DictionaryResource topRes = loadTopRes();
    load((DialogResource) topRes.child("AboutDialog"));
    setTitle("About " + parent.getTitle());

    init(ProductInfo.getInstance());

    Button okButton = (Button) findComponent("OKButton");
    getRootPane().setDefaultButton(okButton);
    okButton.addActionListener(this);
  }

  public void actionPerformed(ActionEvent ae) {
    setVisible(false);
  }

  private String versionText(ProductInfo productInfo) {
    StringBuffer sb = new StringBuffer("<html><p>");
    sb.append(productInfo.toLongString());
    if (productInfo.isPatched()) {
      sb.append("<p style=\"text-align:center\">");
      sb.append(productInfo.toLongPatchString());
    }
    sb.append("</html>");
    return sb.toString();
  }

  private void init(ProductInfo productInfo) {
    Label versionLabel = (Label) findComponent("VersionLabel");
    XTextArea systemInfoArea = (XTextArea) findComponent("SystemInfoTextArea");
    Label copyrightLabel = (Label) findComponent("CopyrightLabel");

    String newLine = System.getProperty("line.separator");
    versionLabel.setText(versionText(productInfo));
    String osInfo = System.getProperty("os.name") + " (" + System.getProperty("os.version") + "/"
                    + System.getProperty("os.arch") + ")";
    String javaVersion = "Java " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
    String javaHomeDir = System.getProperty("java.home");
    String javaVMInfo = System.getProperty("java.vm.name") + ", " + System.getProperty("java.vm.version") + " ["
                        + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB]";

    systemInfoArea.setText(osInfo + newLine + javaVersion + newLine + javaHomeDir + newLine + javaVMInfo);

    copyrightLabel.setText(productInfo.copyright());
  }

  private DictionaryResource loadTopRes() {
    InputStream is = getClass().getResourceAsStream("AboutDialog.xml");
    DictionaryResource topRes = null;

    try {
      topRes = ApplicationManager.loadResource(is);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return topRes;
  }

}
