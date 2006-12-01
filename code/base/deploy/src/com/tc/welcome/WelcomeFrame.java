/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import org.dijon.ApplicationManager;

import com.tc.admin.common.InputStreamDrainer;
import com.tc.util.BundleHelper;

import java.awt.Cursor;
import java.io.File;
import java.net.URL;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

public class WelcomeFrame extends HyperlinkFrame {
  private static BundleHelper m_bundleHelper = new BundleHelper(WelcomeFrame.class);
  
  public WelcomeFrame() {
    super();
    setTitle(m_bundleHelper.getString(getProduct().toLowerCase()+".welcome.title"));
  }
  
  protected URL getPage() {
    return getClass().getResource("Welcome"+getProduct()+".html");
  }
  
  private void runDSOSampleLauncher() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    
    try {
      String[] cmdarray = {getJavaCmd().getAbsolutePath(),
                           "-Dtc.config=tc-config.xml", 
                           "-Dtc.install-root="+System.getProperty("tc.install-root"),
                           "-cp", getTCLib().getAbsolutePath(),
                           "com.tc.welcome.DSOSamplesFrame"};

      Process            p          = exec(cmdarray, null, getSamplesDir());
      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());
      
      errDrainer.start();
      outDrainer.start();
      startFakeWaitPeriod();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  protected void hyperlinkActivated(AttributeSet anchor, String action) {
    if(action.equals("show_samples")) {
      if("DSO".equals(getProduct())) {
        runDSOSampleLauncher();
      } else {
        File file = new File(getProductDirectory(), "samples");
        file = new File(file, "samples.html");
        
        openURL(file.getAbsolutePath());
      }
    }
    else if(action.equals("run_console")) {
      startFakeWaitPeriod();
      runScript("tc-admin");
    }
    else if(action.equals("run_configurator")) {
      startFakeWaitPeriod();
      runScript("tc-configurator");
    }
    else if(action.equals("show_guide")) {
      File   file = new File(getProductDirectory(), "docs");
      String doc  = (String)anchor.getAttribute(HTML.Attribute.NAME);

      file = new File(file, doc);
      
      openURL(file.getAbsolutePath());
    }
    else {
      openURL(action);
    }
  }

  public static void main(String[] args) {
    ApplicationManager.parseLAFArgs(args);
    WelcomeFrame frame = new WelcomeFrame();
    frame.setResizable(false);
  }
}
