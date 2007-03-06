/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import org.dijon.ApplicationManager;
import org.dijon.TabbedPane;
import org.dijon.TextPane;

import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.InputStreamDrainer;
import com.tc.util.ResourceBundleHelper;
import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
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

import javax.swing.Timer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class WelcomeFrame extends HyperlinkFrame implements HyperlinkListener, PropertyChangeListener {
  private static String[]     PRODUCTS       = { "Pojo", "Spring", "Sessions" };
  private static ResourceBundleHelper m_bundleHelper = new ResourceBundleHelper(WelcomeFrame.class);
  private TabbedPane          m_tabbedPane;
  private SimpleAttributeSet  m_underlineAttrSet;
  private ArrayList           m_startupList;

  public WelcomeFrame() {
    super();

    setTitle(getBundleString("welcome.title"));

    m_startupList = new ArrayList();

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(m_tabbedPane = new TabbedPane());

    m_underlineAttrSet = new SimpleAttributeSet();

    addWindowListener(new WindowAdapter() {
      public void windowDeactivated(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }

      public void windowActivated(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }

      public void windowGainedFocus(WindowEvent e) {
        setTextPaneCursor(Cursor.DEFAULT_CURSOR);
      }
    });

    for (int i = 0; i < PRODUCTS.length; i++) {
      TextPane textPane = new TextPane();
      m_startupList.add(textPane);
      textPane.setEditable(false);
      textPane.addHyperlinkListener(this);
      textPane.addPropertyChangeListener("page", this);
      try {
        textPane.setPage(getClass().getResource("Welcome" + PRODUCTS[i] + ".html"));
      } catch (IOException ioe) {
        textPane.setText(ioe.getMessage());
      }

      m_tabbedPane.add(PRODUCTS[i], textPane);
    }
  }

  private String getBundleString(String key) {
    return m_bundleHelper.getString(key);
  }

  private TextPane getTextPane() {
    return (TextPane) m_tabbedPane.getSelectedComponent();
  }

  protected void setTextPaneCursor(int type) {
    Cursor c = Cursor.getPredefinedCursor(type);
    TextPane textPane = getTextPane();
    HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();

    textPane.setCursor(c);
    kit.setDefaultCursor(c);

    int linkType = (type == Cursor.WAIT_CURSOR) ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR;
    kit.setLinkCursor(Cursor.getPredefinedCursor(linkType));
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    TextPane textPane = getTextPane();
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null) { return; }

    if (type == HyperlinkEvent.EventType.ENTERED) {
      underlineElementText(elem, true);
      return;
    } else if (type == HyperlinkEvent.EventType.EXITED) {
      underlineElementText(elem, false);
      return;
    }

    if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet a = elem.getAttributes();
      AttributeSet anchor = (AttributeSet) a.getAttribute(HTML.Tag.A);
      String action = (String) anchor.getAttribute(HTML.Attribute.HREF);

      hyperlinkActivated(anchor, action);
    }
  }

  private String getProduct() {
    return PRODUCTS[m_tabbedPane.getSelectedIndex()];
  }

  private void runDSOSampleLauncher() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);

    try {
      String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Dtc.config=tc-config.xml",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(),
          "-cp", getTCLib().getAbsolutePath(),
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
      if ("Pojo".equals(getProduct())) {
        runDSOSampleLauncher();
      } else {
        File file = new File(getProductDirectory(), "samples.html");

        openURL("file://" + file.getAbsolutePath());
      }
    } else if (action.equals("run_console")) {
      startFakeWaitPeriod();
      runScript("admin");
    } else if (action.equals("run_configurator")) {
      startFakeWaitPeriod();
      runScript("sessions-configurator", "tools" + System.getProperty("file.separator") + "sessions");
    } else if (action.equals("show_guide")) {
      File file = new File(getProductDirectory(), "docs");
      String doc = (String) anchor.getAttribute(HTML.Attribute.NAME);

      file = new File(file, doc);

      openURL(file.getAbsolutePath());
    } else {
      openURL(action);
    }
  }

  protected File getProductDirectory() {
    int index = m_tabbedPane.getSelectedIndex();
    return new File(getSamplesDir(), PRODUCTS[index].toLowerCase());
  }

  private void underlineElementText(Element elem, boolean b) {
    TextPane textPane = getTextPane();
    DefaultStyledDocument doc = (DefaultStyledDocument) textPane.getDocument();
    int start = elem.getStartOffset();
    int len = elem.getEndOffset() - start;

    if (len > 1) {
      StyleConstants.setUnderline(m_underlineAttrSet, b);
      doc.setCharacterAttributes(start, len, m_underlineAttrSet, false);
    }
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
    TextPane textPane = (TextPane) pce.getSource();
    m_startupList.remove(textPane);

    if (m_startupList.isEmpty()) {
      pack();
      center();
      setVisible(true);
    }
  }

  public static void main(String[] args) {
    ApplicationManager.parseLAFArgs(args);
    WelcomeFrame welcome = new WelcomeFrame();
    welcome.setResizable(false);
  }
}
