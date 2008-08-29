/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

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
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Timer;
import javax.swing.UIManager;

public class AdminClient extends ApplicationManager {
  private static AdminClient  m_client;
  private AdminClientContext  m_cntx;

  private static final String PREF_FILE = ".AdminClient.xml";

  static {
    if (!Boolean.getBoolean("javax.management.remote.debug")) {
      // Silence jmx remote
      Logger.getLogger("javax.management.remote").setLevel(Level.OFF);
      Logger.getLogger("com.sun.jmx.remote.opt.util").setLevel(Level.OFF);
    }

    // Silence httpclient
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
  }

  private AdminClient() {
    super("AdminClient");

    if (Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }

    m_client = this;
    m_cntx = new AdminClientContext(this, new ResourceBundleHelper(getClass()), loadTopRes(), AbstractNodeFactory
        .getFactory(), loadPrefs(), Executors.newCachedThreadPool());
    setIconImage(new Image(getClass().getResource("/com/tc/admin/icons/logo_small.png")));
  }

  public static synchronized AdminClient getClient() {
    if (m_client == null) {
      m_client = new AdminClient();
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
   * We use java.util.prefs instead of Dijon resources.
   */
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
    } catch (RuntimeException re) {
      // ignore
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
      m_cntx.getPrefs().exportSubtree(fos);
      m_cntx.getPrefs().flush();
    } catch (Exception e) {
      /**/
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  private DictionaryResource loadTopRes() {
    DictionaryResource topRes = null;
    InputStream is = null;

    try {
      is = getClass().getResourceAsStream("AdminClient.xml");
      topRes = ApplicationManager.loadResource(is);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    return topRes;
  }

  public void start() {
    final AdminClientFrame frame = new AdminClientFrame();
    frame.setIconImage(getIconImage());
    m_cntx.setController(frame);
    Timer t = new Timer(splashProc != null ? 1000 : 0, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        frame.setVisible(true);
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
      AdminClient client = new AdminClient();
      String[] finalArgs;
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
      splashProc = Splash.start("Starting Terracotta AdminConsole...", starter);
      splashProc.waitFor();
    } else {
      new StartupAction(args).run();
    }
  }
}
