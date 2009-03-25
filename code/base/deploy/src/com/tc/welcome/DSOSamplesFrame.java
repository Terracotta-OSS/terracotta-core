/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import com.tc.admin.TCStop;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.LAFHelper;
import com.tc.admin.common.Splash;
import com.tc.admin.common.StreamReader;
import com.tc.admin.common.TextPaneUpdater;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XFrame;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.server.ServerConstants;
import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class DSOSamplesFrame extends HyperlinkFrame implements HyperlinkListener, PropertyChangeListener {
  private static ResourceBundleHelper bundleHelper = new ResourceBundleHelper(DSOSamplesFrame.class);
  private XTextPane                   textPane;
  private XTextPane                   outputPane;
  private ArrayList                   processList;

  static {
    if (Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }
  }

  public DSOSamplesFrame(String[] args) {
    super(bundleHelper.getString("frame.title"));

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());

    textPane = new XTextPane();
    textPane.setBackground(Color.WHITE);
    cp.add(new XScrollPane(textPane));
    textPane.setEditable(false);
    textPane.addHyperlinkListener(this);
    textPane.addPropertyChangeListener("page", this);

    processList = new ArrayList();
    outputPane = new XTextPane();
    outputPane.setForeground(Color.GREEN);
    outputPane.setBackground(Color.BLACK);
    outputPane.setFont(Font.decode("Monospaced-plain-10"));
    XScrollPane scroller = new XScrollPane(outputPane);
    scroller.setPreferredSize(new Dimension(500, 200));
    cp.add(scroller, BorderLayout.SOUTH);
    runServer();

    try {
      textPane.setPage(DSOSamplesFrame.class.getResource("SamplesPojo.html"));
    } catch (IOException ioe) {
      textPane.setText(ioe.getMessage());
    }
  }

  protected void initFileMenu(JMenu fileMenu) {
    fileMenu.add(new ServersAction());
    super.initFileMenu(fileMenu);
  }

  class ServersAction extends XAbstractAction {
    JPanel        panel;
    JToggleButton useLocalToggle;
    JTextField    serversListField;

    ServersAction() {
      super(bundleHelper.getString("servers.action.name"));
    }

    private JPanel createPanel() {
      if (panel == null) {
        panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(useLocalToggle = new JCheckBox(bundleHelper.getString("servers.use.local")));
        useLocalToggle.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            serversListField.setEnabled(!useLocalToggle.isSelected());
          }
        });
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new FlowLayout());
        otherPanel.add(new JLabel(bundleHelper.getString("servers.use.remote")));
        serversListField = new JTextField("server1:9510,server2:9510,server3:9510");
        serversListField.setToolTipText(bundleHelper.getString("servers.field.tip"));
        serversListField.setPreferredSize(serversListField.getPreferredSize());
        String prop = System.getProperty("tc.server");
        serversListField.setText(prop);
        otherPanel.add(serversListField);
        bottomPanel.add(otherPanel, BorderLayout.CENTER);
        JLabel serversListDescription = new JLabel(bundleHelper.getString("servers.field.description"));
        serversListDescription.setBorder(new EmptyBorder(0, 20, 0, 0));
        bottomPanel.add(serversListDescription, BorderLayout.SOUTH);
        panel.add(bottomPanel);

        serversListField.setEnabled(prop != null);
        useLocalToggle.setSelected(prop == null);
      }
      return panel;
    }

    public void actionPerformed(ActionEvent ae) {
      int result;
      result = JOptionPane.showConfirmDialog(DSOSamplesFrame.this, createPanel(), DSOSamplesFrame.this.getTitle(),
                                             JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        if (!useLocalToggle.isSelected()) {
          System.setProperty("tc.server", serversListField.getText());
        } else {
          System.getProperties().remove("tc.server");
        }
      }
    }
  }

  private void toOutputPane(String s) {
    try {
      Document doc = outputPane.getDocument();
      doc.insertString(doc.getLength(), s + "\n", null);
    } catch (BadLocationException ble) {
      throw new AssertionError(ble);
    }
  }

  protected void quit() {
    stopProcesses();
  }

  private StreamReader createStreamReader(InputStream stream) {
    return new StreamReader(stream, new TextPaneUpdater(outputPane), null, null);
  }

  private StreamReader createStreamReader(InputStream stream, JTextPane theTextPane) {
    return new StreamReader(stream, new TextPaneUpdater(theTextPane), null, null);
  }

  private void stopProcesses() {
    String host = "localhost";
    int port = 9520;

    try {
      new TCStop(host, port).stop();
    } catch (Exception e) {
      toOutputPane(e.getMessage());
    }

    Iterator iter = processList.iterator();
    while (iter.hasNext()) {
      Process p = (Process) iter.next();

      try {
        p.exitValue();
      } catch (Exception e) {
        p.destroy();
      }
    }

    new Thread() {
      public void run() {
        TCStop tester = new TCStop("localhost", 9520);

        while (true) {
          try {
            sleep(2000);
            tester.stop();
          } catch (Exception e) {
            DSOSamplesFrame.super.quit();
          }
        }
      }
    }.start();
  }

  private void runServer() {
    try {
      File bootFile = new File(getBootPath());

      if (bootFile.exists()) {
        internalRunServer();
      } else {
        createBootJar();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void createBootJar() {
    String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(),
        "-cp", getTCLib().getAbsolutePath(), "com.tc.object.tools.BootJarTool" };

    final Process p = exec(cmdarray, null, getSamplesDir());
    StreamReader errDrainer = createStreamReader(p.getErrorStream());
    StreamReader outDrainer = createStreamReader(p.getInputStream());

    errDrainer.start();
    outDrainer.start();

    new Thread() {
      public void run() {
        while (true) {
          try {
            p.waitFor();
            break;
          } catch (Exception e) {/**/
          }
        }
        internalRunServer();
      }
    }.start();
  }

  private void internalRunServer() {
    String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Xms256m", "-Xmx256m",
        "-Dcom.sun.management.jmxremote", "-Dtc.config=tc-config.xml",
        "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(), "-cp", getTCLib().getAbsolutePath(),
        ServerConstants.SERVER_MAIN_CLASS_NAME };

    Process p = exec(cmdarray, null, getSamplesDir());
    StreamReader errDrainer = createStreamReader(p.getErrorStream());
    StreamReader outDrainer = createStreamReader(p.getInputStream());

    errDrainer.start();
    outDrainer.start();
  }

  private void runSample(String dirName, String className) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String bootPath = getBootPath();
      String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dcom.tc.l1.max.connect.retries=3",
          "-Dtc.config=tc-config.xml", "-Djava.awt.Window.locationByPlatform=true",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(),
          "-Dtc.server=" + System.getProperty("tc.server", ""), "-Xbootclasspath/p:" + bootPath, "-cp", "classes",
          className };

      Process p = exec(cmdarray, null, new File(getProductDirectory(), dirName));
      StreamReader errDrainer = createStreamReader(p.getErrorStream());
      StreamReader outDrainer = createStreamReader(p.getInputStream());

      errDrainer.start();
      outDrainer.start();

      processList.add(p);
      startFakeWaitPeriod();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void runCoordinationSample() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String bootPath = getBootPath();
      File dir = new File(getProductDirectory(), "coordination");
      String classpath = getLibClassPath(dir, "classes");
      String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dcom.tc.l1.max.connect.retries=3",
          "-Dtc.config=tc-config.xml", "-Djava.awt.Window.locationByPlatform=true",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(), "-Xbootclasspath/p:" + bootPath, "-cp", classpath,
          "demo.coordination.Main" };

      final Process p = exec(cmdarray, null, dir);
      XTextPane streamTextPane = new XTextPane();
      StreamReader errDrainer = createStreamReader(p.getErrorStream(), streamTextPane);
      StreamReader outDrainer = createStreamReader(p.getInputStream(), streamTextPane);

      errDrainer.start();
      outDrainer.start();

      processList.add(p);
      startFakeWaitPeriod();

      SampleFrame frame = new SampleFrame(this, bundleHelper.getString("jvm.coordination"));
      frame.getContentPane().add(new XScrollPane(streamTextPane));
      frame.setSize(new Dimension(500, 300));
      frame.setVisible(true);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          try {
            p.destroy();
          } catch (Exception e) {/**/
          }
          processList.remove(p);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getLibClassPath(final File dir, final String defaultPath) {
    final String pathSep = System.getProperty("path.separator");
    final String fileSep = System.getProperty("file.separator");
    final String LIB_DIR = "lib";
    final File libdir = new File(dir, LIB_DIR);
    String classpath = defaultPath;
    if (libdir.exists()) {
      final String[] jars = libdir.list(new FilenameFilter() {
        public boolean accept(File directory, String name) {
          return name.endsWith(".jar");
        }
      });
      for (int i = 0; i < jars.length; i++) {
        classpath += (pathSep + LIB_DIR + fileSep + jars[i]);
      }
    }
    return classpath;
  }

  private void runSharedQueueSample() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String bootPath = getBootPath();
      File dir = new File(getProductDirectory(), "sharedqueue");
      String classpath = getLibClassPath(dir, "classes");
      String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dcom.tc.l1.max.connect.retries=3",
          "-Dtc.config=tc-config.xml", "-Djava.awt.Window.locationByPlatform=true",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(), "-Xbootclasspath/p:" + bootPath, "-cp", classpath,
          "demo.sharedqueue.Main" };

      final Process p = exec(cmdarray, null, dir);
      XTextPane streamTextPane = new XTextPane();
      StreamReader errDrainer = createStreamReader(p.getErrorStream(), streamTextPane);
      StreamReader outDrainer = createStreamReader(p.getInputStream(), streamTextPane);

      errDrainer.start();
      outDrainer.start();

      processList.add(p);
      startFakeWaitPeriod();

      SampleFrame frame = new SampleFrame(this, bundleHelper.getString("shared.work.queue"));
      frame.getContentPane().add(new XScrollPane(streamTextPane));
      frame.setSize(new Dimension(500, 300));
      frame.setVisible(true);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          try {
            p.destroy();
          } catch (Exception e) {/**/
          }
          processList.remove(p);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
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

  private void hyperlinkActivated(AttributeSet anchor, String action) {
    if (action.equals("run_jtable")) {
      toOutputPane(bundleHelper.getString("starting.jtable"));
      runSample("jtable", "demo.jtable.Main");
    } else if (action.equals("run_sharededitor")) {
      toOutputPane(bundleHelper.getString("starting.shared.editor"));
      runSample("sharededitor", "demo.sharededitor.Main");
    } else if (action.equals("run_chatter")) {
      toOutputPane(bundleHelper.getString("starting.chatter"));
      runSample("chatter", "demo.chatter.Main");
    } else if (action.equals("run_coordination")) {
      toOutputPane(bundleHelper.getString("starting.jvm.coordination"));
      runCoordinationSample();
    } else if (action.equals("run_sharedqueue")) {
      toOutputPane(bundleHelper.getString("starting.shared.queue"));
      runSharedQueueSample();
    } else if (action.equals("view_readme")) {
      String name = (String) anchor.getAttribute(HTML.Attribute.NAME);
      File dir = new File(getProductDirectory(), name);
      File file = new File(dir, "readme.html");

      openURL("file://" + file.getAbsolutePath());
    } else if (action.equals("browse_source")) {
      String name = (String) anchor.getAttribute(HTML.Attribute.NAME);
      File sampleDir = new File(getProductDirectory(), name);
      File sampleDocs = new File(sampleDir, "docs");
      File index = new File(sampleDocs, "source.html");

      openURL("file://" + index.getAbsolutePath());
    } else {
      openURL(action);
    }
  }

  private void setTextPaneCursor(int type) {
    Cursor c = Cursor.getPredefinedCursor(type);
    HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();

    textPane.setCursor(c);
    kit.setDefaultCursor(c);

    int linkType = (type == Cursor.WAIT_CURSOR) ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR;
    kit.setLinkCursor(Cursor.getPredefinedCursor(linkType));
  }

  private void openURL(String url) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    BrowserLauncher.openURL(url);
    startFakeWaitPeriod();
  }

  private void startFakeWaitPeriod() {
    Timer t = new Timer(3000, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }
    });
    t.setRepeats(false);
    t.start();
  }

  private File getProductDirectory() {
    return new File(getSamplesDir(), "pojo");
  }

  public void propertyChange(PropertyChangeEvent pce) {
    Timer t = new Timer(splashProc != null ? 1000 : 0, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        pack();
        center();
        setVisible(true);
        if (splashProc != null) {
          splashProc.destroy();
        }
      }
    });
    t.setRepeats(false);
    t.start();
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
      new DSOSamplesFrame(finalArgs);
    }
  }

  public static void main(final String[] args) throws Exception {
    if (System.getProperty("swing.defaultlaf") == null) {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    List<String> argList = Arrays.asList(args);
    if (argList.remove("-showSplash")) {
      StartupAction starter = new StartupAction(argList.toArray(new String[argList.size()]));
      splashProc = Splash.start("Starting Pojo Sample Launcher...", starter);
      splashProc.waitFor();
    } else {
      new StartupAction(args).run();
    }
  }
}

class SampleFrame extends XFrame {
  public SampleFrame(JFrame parentFrame, String title) {
    super(title);
    getContentPane().setLayout(new BorderLayout());
  }
}
