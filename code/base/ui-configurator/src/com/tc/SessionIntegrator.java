/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.commons.io.IOUtils;
import org.dijon.ApplicationManager;
import org.dijon.DictionaryResource;
import org.dijon.Image;

import com.tc.util.BundleHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.prefs.Preferences;

public class SessionIntegrator extends ApplicationManager {
  private static SessionIntegrator m_client;
  private SessionIntegratorContext m_cntx;

  private static final String APP_NAME =  "SessionIntegrator";
  private static final String PREF_FILE = "."+APP_NAME+".xml";
  
  public SessionIntegrator() {
    super(APP_NAME);
    
    m_cntx = new SessionIntegratorContext();
    m_cntx.client = m_client = this;
    m_cntx.prefs = loadPrefs();
    m_cntx.topRes = loadTopRes();
    m_cntx.bundleHelper = new BundleHelper(getClass());

    setIconImage(new Image(getBytes("/com/tc/admin/icons/logo_small.gif")));
  }
  
  static byte[] getBytes(String path) {
    byte[] result = null;
    URL    url    = SessionIntegrator.class.getResource(path);
    
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
  
  public void toConsole(String msg) {
    /**/
  }
  
  public static SessionIntegrator getClient() {
    return m_client;
  }

  protected SessionIntegratorContext context() {
    return m_cntx;
  }

  public static SessionIntegratorContext getContext() {
    return getClient().context();
  }

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
      is = getClass().getResourceAsStream(APP_NAME+".xml");
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
    m_cntx.frame = new SessionIntegratorFrame();
    m_cntx.frame.setVisible(true);
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

    SessionIntegrator client = new SessionIntegrator();
    client.parseArgs(appArgs);
    client.start();
  }
}
