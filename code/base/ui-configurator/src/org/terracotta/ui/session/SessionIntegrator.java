/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.commons.io.IOUtils;
import org.dijon.ApplicationManager;
import org.dijon.DictionaryResource;
import org.dijon.Image;

import com.tc.admin.common.Splash;
import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.Timer;
import javax.swing.UIManager;

public class SessionIntegrator extends ApplicationManager {
  private static SessionIntegrator m_client;
  private SessionIntegratorContext m_cntx;

  private static final String      APP_NAME  = "SessionIntegrator";
  private static final String      PREF_FILE = "." + APP_NAME + ".xml";

  public SessionIntegrator() {
    super(APP_NAME);

    if (Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }

    m_cntx = new SessionIntegratorContext();
    m_cntx.client = m_client = this;
    m_cntx.prefs = loadPrefs();
    m_cntx.topRes = loadTopRes();
    m_cntx.bundleHelper = new ResourceBundleHelper(getClass());

    setIconImage(new Image(getBytes("/com/tc/admin/icons/logo_small.gif")));
  }

  static byte[] getBytes(String path) {
    byte[] result = null;
    URL url = SessionIntegrator.class.getResource(path);

    if (url != null) {
      InputStream is = null;

      try {
        result = IOUtils.toByteArray(is = url.openStream());
      } catch (IOException ioe) {
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

  public void storePreferences() {/**/
  }

  private Preferences loadPrefs() {
    FileInputStream fis = null;

    try {
      File f = new File(System.getProperty("user.home"), PREF_FILE);

      if (f.exists()) {
        fis = new FileInputStream(f);
        Preferences.importPreferences(fis);
      }
    } catch (Exception e) {
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
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  private DictionaryResource loadTopRes() {
    DictionaryResource topRes = null;
    InputStream is = null;

    try {
      is = getClass().getResourceAsStream(APP_NAME + ".xml");
      topRes = ApplicationManager.loadResource(is);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(-1);
    } finally {
      IOUtils.closeQuietly(is);
    }

    return topRes;
  }

  public void start() {
    m_cntx.frame = new SessionIntegratorFrame();
    Timer t = new Timer(splashProc != null ? 1000 : 0, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_cntx.frame.setVisible(true);
        if (splashProc != null) {
          splashProc.destroy();
        }
      }
    });
    t.setRepeats(false);
    t.start();
  }

  public String[] parseArgs(String[] args) {
    args = super.parseArgs(args);

    if (args != null && args.length > 0) {
      // There may be arguments in the future
    }

    return args;
  }

  private static Process splashProc;

  private static class StartupAction implements Runnable {
    private final String[] args;

    StartupAction(String[] args) {
      this.args = args;
    }

    public void run() {
      String[] finalArgs;
      SessionIntegrator client = new SessionIntegrator();
      if (System.getProperty("swing.defaultlaf") == null) {
        finalArgs = ApplicationManager.parseLAFArgs(args);
      } else {
        finalArgs = args;
      }
      client.parseArgs(finalArgs);
      client.start();
    }
  }

  public static final void main(final String[] args) throws Exception {
    if (System.getProperty("swing.defaultlaf") == null) {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    List<String> argList = Arrays.asList(args);
    if (argList.remove("-showSplash")) {
      StartupAction starter = new StartupAction(argList.toArray(new String[argList.size()]));
      splashProc = Splash.start("Starting Terracotta Sessions Configurator...", starter);
      splashProc.waitFor();
    } else {
      new StartupAction(args).run();
    }
  }
}
