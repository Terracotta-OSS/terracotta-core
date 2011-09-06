/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import com.tc.admin.common.AboutDialog;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XFrame;
import com.tc.object.tools.BootJarSignature;
import com.tc.object.tools.UnsupportedVMException;
import com.tc.util.ProductInfo;
import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;
import javax.swing.JPopupMenu.Separator;
import javax.swing.event.HyperlinkListener;

public abstract class HyperlinkFrame extends XFrame implements HyperlinkListener {
  private static ResourceBundleHelper bundleHelper = new ResourceBundleHelper(HyperlinkFrame.class);

  private File                        installRoot;
  private File                        bootPath;
  private File                        javaCmd;
  private File                        tcLib;
  private File                        samplesDir;

  public HyperlinkFrame(String title) {
    super(title);

    JMenuBar menubar = new JMenuBar();
    JMenu menu;

    setJMenuBar(menubar);
    menubar.add(menu = new JMenu(getBundleString("file.menu.title")));
    initFileMenu(menu);
    menubar.add(menu = new JMenu(getBundleString("help.menu.title")));

    String kitID = ProductInfo.getInstance().kitID();
    menu
        .add(new ContactTerracottaAction(getBundleString("visit.forums.title"), formatBundleString("forums.url", kitID)));
    menu.add(new ContactTerracottaAction(getBundleString("contact.support.title"), formatBundleString("support.url",
                                                                                                      kitID)));
    menu.add(new Separator());
    menu.add(new AboutAction());

    setIconImage(Toolkit.getDefaultToolkit().getImage(
                                                      HyperlinkFrame.class
                                                          .getResource("/com/tc/admin/icons/logo_small.png")));

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        quit();
      }
    });
  }

  protected void initFileMenu(JMenu fileMenu) {
    fileMenu.add(new QuitAction());
  }

  private String getBundleString(String key) {
    return bundleHelper.getString(key);
  }

  private String formatBundleString(String key, Object... args) {
    return MessageFormat.format(getBundleString(key), args);
  }

  protected void quit() {
    Runtime.getRuntime().exit(0);
  }

  protected File getInstallRoot() {
    if (installRoot == null) {
      installRoot = new File(System.getProperty("tc.install-root").trim());
    }
    return installRoot;
  }

  protected String getBootPath() throws UnsupportedVMException {
    if (bootPath == null) {
      File path = new File(getInstallRoot(), "lib");
      path = new File(path, "dso-boot");
      path = new File(path, BootJarSignature.getBootJarNameForThisVM());
      this.bootPath = path;
    }

    return bootPath.getAbsolutePath();
  }

  protected static String getenv(String key) {
    try {
      Method m = System.class.getMethod("getenv", new Class[] { String.class });
      if (m != null) { return (String) m.invoke(null, new Object[] { key }); }
    } catch (Throwable t) {/**/
    }

    return null;
  }

  static File staticGetJavaCmd() {
    File javaBin = new File(System.getProperty("java.home"), "bin");
    return new File(javaBin, "java" + (Os.isWindows() ? ".exe" : ""));
  }

  protected File getJavaCmd() {
    if (javaCmd == null) {
      javaCmd = staticGetJavaCmd();
    }

    return javaCmd;
  }

  static File staticGetTCLib() {
    File file = new File(System.getProperty("tc.install-root").trim());
    file = new File(file, "lib");
    return new File(file, "tc.jar");
  }

  protected File getTCLib() {
    if (tcLib == null) {
      tcLib = staticGetTCLib();
    }
    return tcLib;
  }

  protected File getSamplesDir() {
    if (samplesDir == null) {
      File platformDir = new File(getInstallRoot(), "platform");
      samplesDir = new File(platformDir, "samples");
    }
    return samplesDir;
  }

  protected Process exec(String[] cmdarray, String[] envp, File dir) {
    try {
      return Runtime.getRuntime().exec(cmdarray, envp, dir);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return null;
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(getBundleString("quit.action.name"));
    }

    public void actionPerformed(ActionEvent ae) {
      quit();
    }
  }

  class AboutAction extends XAbstractAction {
    AboutDialog aboutDialog;

    AboutAction() {
      super(getBundleString("about.title.prefix") + getTitle());
    }

    public void actionPerformed(ActionEvent ae) {
      if (aboutDialog == null) {
        aboutDialog = new AboutDialog(HyperlinkFrame.this);
      }

      aboutDialog.pack();
      WindowHelper.center(aboutDialog, HyperlinkFrame.this);
      aboutDialog.setVisible(true);
    }
  }
}
