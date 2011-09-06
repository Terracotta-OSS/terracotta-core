/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.InputStreamDrainer;
import com.tc.admin.common.LAFHelper;
import com.tc.admin.common.Splash;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class WelcomeFrame extends HyperlinkFrame implements HyperlinkListener, PropertyChangeListener {
  private static ResourceBundleHelper bundleHelper = new ResourceBundleHelper(WelcomeFrame.class);

  private XTextPane                   textPane;
  private final ArrayList             startupList;

  public WelcomeFrame(String[] args) {
    super(getBundleString("welcome.title"));

    if (Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }

    startupList = new ArrayList();

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(new XScrollPane(textPane = new XTextPane()));

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowDeactivated(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }

      @Override
      public void windowActivated(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }

      @Override
      public void windowGainedFocus(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }
    });

    startupList.add(textPane);
    textPane.setBackground(Color.WHITE);
    textPane.setEditable(false);
    textPane.addHyperlinkListener(this);
    textPane.addPropertyChangeListener("page", this);
    try {
      textPane.setPage(getClass().getResource("Welcome.html"));
    } catch (IOException ioe) {
      textPane.setText(ioe.getMessage());
    }
  }

  private static String getBundleString(String key) {
    return bundleHelper.getString(key);
  }

  protected void setTextPaneCursor(int type) {
    Cursor c = Cursor.getPredefinedCursor(type);
    HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();

    textPane.setCursor(c);
    kit.setDefaultCursor(c);

    int linkType = (type == Cursor.WAIT_CURSOR) ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR;
    kit.setLinkCursor(Cursor.getPredefinedCursor(linkType));
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

    if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet a = elem.getAttributes();
      AttributeSet anchor = (AttributeSet) a.getAttribute(HTML.Tag.A);
      String action = (String) anchor.getAttribute(HTML.Attribute.HREF);

      hyperlinkActivated(anchor, action);
    }
  }

  private void runDSOSampleLauncher() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);

    try {
      String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dtc.config=tc-config.xml",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(), "-cp", getTCLib().getAbsolutePath(),
          "com.tc.welcome.DSOSamplesFrame" };

      Process p = exec(cmdarray, null, getProductDirectory());
      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());

      errDrainer.start();
      outDrainer.start();
      startFakeWaitPeriod();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void hyperlinkActivated(AttributeSet anchor, String action) {
    if (action.equals("show_samples")) {
      runDSOSampleLauncher();
    } else if (action.equals("run_dev_center")) {
      startFakeWaitPeriod();
      runScript("dev-console");
    } else if (action.equals("run_configurator")) {
      startFakeWaitPeriod();
      runScript("sessions-configurator", "tools" + System.getProperty("file.separator") + "sessions");
    } else {
      openURL(action);
    }
  }

  protected File getProductDirectory() {
    return new File(getSamplesDir(), "pojo");
  }

  protected void openURL(String url) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    BrowserLauncher.openURL(url);
    startFakeWaitPeriod();
  }

  protected void runSampleScript(String scriptPath) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);

    File dir = getProductDirectory();
    String ext = Os.isWindows() ? ".bat" : ".sh";
    File file = new File(dir, scriptPath + ext);
    String[] cmd = { file.getAbsolutePath() };

    try {
      Process p = Runtime.getRuntime().exec(cmd);

      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());

      errDrainer.start();
      outDrainer.start();

      startFakeWaitPeriod();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  protected void runScript(String scriptName) {
    runScript(scriptName, "bin");
  }

  protected void runScript(String scriptName, String scriptRoot) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);

    File dir = new File(getInstallRoot(), scriptRoot);
    String ext = Os.isWindows() ? ".bat" : ".sh";
    File file = new File(dir, scriptName + ext);
    String[] cmd = { file.getAbsolutePath() };

    try {
      Process p = Runtime.getRuntime().exec(cmd);

      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());

      errDrainer.start();
      outDrainer.start();

      startFakeWaitPeriod();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  protected void startFakeWaitPeriod() {
    Timer t = new Timer(3000, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }
    });
    t.setRepeats(false);
    t.start();
  }

  public void propertyChange(PropertyChangeEvent pce) {
    startupList.remove(textPane);

    if (startupList.isEmpty()) {
      pack();
      center();
      Timer t = new Timer(splashProc != null ? 1000 : 0, new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          setVisible(true);
          if (splashProc != null) {
            splashProc.destroy();
          }
        }
      });
      t.setRepeats(false);
      t.start();
    }
  }

  private static Process splashProc;

  private static class StartupAction implements Runnable {
    private final String[] args;

    StartupAction(String[] args) {
      this.args = args;
    }

    public void run() {
      String[] finalArgs = args;
      if (System.getProperty("swing.defaultlaf") == null) {
        finalArgs = LAFHelper.parseLAFArgs(args);
      }
      WelcomeFrame welcome = new WelcomeFrame(finalArgs);
      welcome.setResizable(false);
    }
  }

  public static void main(final String[] args) throws Exception {
    if (System.getProperty("swing.defaultlaf") == null) {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    List<String> argList = Arrays.asList(args);
    if (argList.remove("-showSplash")) {
      StartupAction starter = new StartupAction(argList.toArray(new String[argList.size()]));
      splashProc = Splash.start("Starting Terracotta Welcome...", starter);
      splashProc.waitFor();
    } else {
      new StartupAction(args).run();
    }
  }
}
