/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.io.IOUtils;
import org.dijon.ApplicationManager;
import org.dijon.DictionaryResource;
import org.dijon.Image;

import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class AdminClient extends ApplicationManager {
  private static AdminClient m_client;
  private AdminClientContext m_cntx;

  private static final String PREF_FILE = ".AdminClient.xml";

  static {
    Logger.getLogger("javax.management.remote.generic").setLevel(Level.OFF);
    Logger.getLogger("javax.management.remote.misc").setLevel(Level.OFF);
    Logger.getLogger("com.sun.jmx.remote.opt.util").setLevel(Level.OFF);
    Logger.getLogger("com.sun.jmx.remote.opt.util").setLevel(Level.OFF);
    Logger.getLogger("javax.management.remote.rmi").setLevel(Level.OFF);
  }
  
  protected AdminClient() {
    super("AdminClient");

    if(Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");

      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }

    m_cntx = new AdminClientContext();
    m_cntx.client = m_client = this;
    m_cntx.prefs = loadPrefs();
    m_cntx.topRes = loadTopRes();
    m_cntx.bundleHelper = new ResourceBundleHelper(getClass());

    if(!Boolean.getBoolean("com.tc.ui.java-icon")) {
      setIconImage(new Image(getBytes("/com/tc/admin/icons/logo_small.gif")));
    }
  }

  static byte[] getBytes(String path) {
    byte[] result = null;
    URL    url    = AdminClient.class.getResource(path);
    
    if(url != null) {
      InputStream is = null;
      
      try {
        result = IOUtils.toByteArray(is = url.openStream());
      } catch(IOException ioe) {
        ioe.printStackTrace();
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    
    return result;
  }

  public static AdminClient getClient() {
    if(m_client == null) {
      new AdminClient().parseArgs(new String[]{});
    }

    return m_client;
  }

  protected AdminClientContext context() {
    return m_cntx;
  }

  public static AdminClientContext getContext() {
    return getClient().context();
  }

  /**
   * We use java.util.prefs instead of Galaxy resources.
   */
  public DictionaryResource loadPreferences() {
    return new DictionaryResource();
  }
  public void storePreferences() {/**/}

  private Preferences loadPrefs() {
    FileInputStream fis = null;

    try {
      File f = new File(System.getProperty("user.home"), PREF_FILE);

      if(f.exists()) {
        fis = new FileInputStream(f);
        Preferences.importPreferences(fis);
      }
    } catch(Exception e) {
      // ignore
    } finally {
      IOUtils.closeQuietly(fis);
    }

    return Preferences.userNodeForPackage(getClass());
  }

  public void storePrefs() {
    FileOutputStream fos = null;

    try {
      File f = new File(System.getProperty("user.home"), PREF_FILE);
      fos = new FileOutputStream(f);
      m_cntx.prefs.exportSubtree(fos);
      m_cntx.prefs.flush();
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  private DictionaryResource loadTopRes() {
    DictionaryResource topRes = null;
    InputStream        is     = null;

    try {
      is = getClass().getResourceAsStream("AdminClient.xml");
      topRes = ApplicationManager.loadResource(is);
    } catch(Throwable t) {
      t.printStackTrace();
      System.exit(-1);
    } finally {
      IOUtils.closeQuietly(is);
    }

    return topRes;
  }

  public void start() {
    m_cntx.controller = new AdminClientFrame();
    ((AdminClientFrame)m_cntx.controller).setVisible(true);
  }

  public String[] parseArgs(String[] args) {
    args = super.parseArgs(args);

    if (args != null && args.length > 0) {
      // There may be arguments in the future
    }

    return args;
  }

  public static final void main(final String[] args)
    throws Exception
  {
    String[] appArgs = ApplicationManager.parseLAFArgs(args);

    AdminClient client = new AdminClient();
    client.parseArgs(appArgs);
    client.start();
  }
}
