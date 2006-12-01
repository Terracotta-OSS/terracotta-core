/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import org.dijon.ApplicationManager;
import org.dijon.DictionaryResource;
import org.dijon.Frame;
import org.dijon.Image;
import org.dijon.Label;
import org.dijon.Menu;
import org.dijon.MenuBar;
import org.dijon.ScrollPane;
import org.dijon.Separator;
import org.dijon.TextPane;

import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.InputStreamDrainer;
import com.tc.admin.common.XAbstractAction;
import com.tc.object.tools.BootJarSignature;
import com.tc.object.tools.UnsupportedVMException;
import com.tc.util.BundleHelper;
import com.tc.util.ProductInfo;
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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public abstract class HyperlinkFrame extends Frame implements HyperlinkListener {
  private static BundleHelper m_bundleHelper = new BundleHelper(HyperlinkFrame.class);
  private TextPane            m_textPane;
  private String              m_product;
  private File                m_productDir;
  private SimpleAttributeSet  m_underlineAttrSet;
    
  public HyperlinkFrame() {
    super();
    
    m_product    = System.getProperty("tc.welcome.product", "dso");
    m_productDir = new File(System.getProperty("tc.install-root"), m_product.toLowerCase());
  
    MenuBar menubar = new MenuBar();
    Menu    menu;
    
    setJMenuBar(menubar);
    menubar.add(menu = new Menu(getBundleString("file.menu.title")));
    menu.add(new QuitAction());
    menubar.add(menu = new Menu(getBundleString("help.menu.title")));
    menu.add(new ContactTerracottaAction(getBundleString("visit.forums.title"),
                                         getBundleString("forums.url")));
    menu.add(new ContactTerracottaAction(getBundleString("contact.support.title"),
                                         getBundleString("support.url")));
    /*
    menu.add(new ContactTerracottaAction(getBundleString("contact.field.eng.title"),
                                         getBundleString("field.eng.url")));
    menu.add(new ContactTerracottaAction(getBundleString("contact.sales.title"),
                                         getBundleString("sales.url")));
    */
    menu.add(new Separator());
    menu.add(new AboutAction());
    
    Container cp = getContentPane();

    m_textPane = new TextPane();
    m_textPane.setEditable(false);
    m_textPane.addHyperlinkListener(this);
    
    cp.setLayout(new BorderLayout());
    cp.add(new ScrollPane(m_textPane));
    
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
    
    URL    url;
    String iconPath = "/com/tc/admin/icons/logo_small.gif";
    
    if((url = getClass().getResource(iconPath)) != null) {
      setIconImage(new Image(url));
    }

    if((url = getPage()) != null) {
      try {
        m_textPane.addPropertyChangeListener("page", new PageListener());
        m_textPane.setPage(url);
      } catch(IOException ioe) {
        ioe.printStackTrace();
        JOptionPane.showConfirmDialog(this, ioe.getLocalizedMessage());
        System.exit(1);
      }
    }
    else {
      JOptionPane.showConfirmDialog(this, getBundleString("page.load.error"));
      System.exit(1);
    }

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        quit();
      }
    });
  }
  
  private String getBundleString(String key) {
    return m_bundleHelper.getString(key);
  }
  
  protected abstract URL getPage();

  protected void quit() {
    System.exit(0);
  }
  
  protected void setTextPaneCursor(int type) {
    Cursor        c   = Cursor.getPredefinedCursor(type);
    HTMLEditorKit kit = (HTMLEditorKit)m_textPane.getEditorKit();
    
    m_textPane.setCursor(c);
    kit.setDefaultCursor(c);
    
    int linkType = (type == Cursor.WAIT_CURSOR) ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR;
    kit.setLinkCursor(Cursor.getPredefinedCursor(linkType));
  }
  
  public void hyperlinkUpdate(HyperlinkEvent e) {
    HyperlinkEvent.EventType type = e.getEventType();
    Element                  elem = e.getSourceElement();
    
    if(elem == null) {
      return;
    }
    
    if(type == HyperlinkEvent.EventType.ENTERED) {
      underlineElementText(elem, true);
      return;
    }
    else if(type == HyperlinkEvent.EventType.EXITED) {
      underlineElementText(elem, false);
      return;
    }
    
    if(m_textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet a       = elem.getAttributes();
      AttributeSet anchor  = (AttributeSet)a.getAttribute(HTML.Tag.A);
      String       action  = (String)anchor.getAttribute(HTML.Attribute.HREF);
      
      hyperlinkActivated(anchor, action);
    }
  }
  
  protected void hyperlinkActivated(AttributeSet anchor, String action) {/**/}
  
  protected String getProduct() {
    return m_product;
  }
  
  protected File getProductDirectory() {
    return m_productDir;
  }
  
  private File m_bootPath;
  protected String getBootPath() throws UnsupportedVMException {
    if(m_bootPath == null) {
      File bootPath = new File(System.getProperty("tc.install-root"), "common");
      bootPath = new File(bootPath, "lib");
      bootPath = new File(bootPath, "dso-boot");
      bootPath = new File(bootPath, BootJarSignature.getBootJarNameForThisVM());
      m_bootPath = bootPath;
    }
    
    return m_bootPath.getAbsolutePath();
  }

  private static String getenv(String key) {
    try {
      Method m = System.class.getMethod("getenv", new Class[] {String.class});
    
      if(m != null) {
        return (String)m.invoke(null, new Object[]{key});
      }
    } catch(Throwable t) {/**/}

    return null;
  }
  

  private File m_javaCmd;
  protected File getJavaCmd() {
    if(m_javaCmd == null) {
      String tcJavaHome = getenv("TC_JAVA_HOME");
      File   file;
      
      if(tcJavaHome != null) {
        file = new File(tcJavaHome, "bin");
      } else {
        file = new File(System.getProperty("tc.install-root"), "jre");
        file = new File(file, "bin");
      }
      
      if(Os.isWindows()) {
        file = new File(file, "java.exe");
      } else {
        file = new File(file, "java");
      }

      m_javaCmd = file;
    }
    return m_javaCmd;
  }
  
  private File m_tcLib;
  protected File getTCLib() {
    if(m_tcLib == null) {
      File file = new File(System.getProperty("tc.install-root"), "common");
      file = new File(file, "lib");
      file = new File(file, "tc.jar");
      m_tcLib = file;
    }
    return m_tcLib;
  }
  
  private File m_samplesDir;
  protected File getSamplesDir() {
    if(m_samplesDir == null) {
      m_samplesDir = new File(getProductDirectory(), "samples");
    }
    return m_samplesDir;
  }

  protected Process exec(String[] cmdarray, String[] envp, File dir) {
    try {
      return Runtime.getRuntime().exec(cmdarray, envp, dir);
    } catch(IOException ioe) {
      ioe.printStackTrace();
    }
    
    return null;
  }
  
  
  private void underlineElementText(Element elem, boolean b) {
    DefaultStyledDocument doc   = (DefaultStyledDocument)m_textPane.getDocument();
    int                   start = elem.getStartOffset();
    int                   len   = elem.getEndOffset()-start;
    
    if(len > 1) {
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
    
    File     dir  = new File(m_productDir, "samples");
    String   ext  = Os.isWindows() ? ".bat" : ".sh";
    File     file = new File(dir, scriptPath+ext);
    String[] cmd  = {file.getAbsolutePath()};
    
    try {
      Process p = Runtime.getRuntime().exec(cmd);

      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());
      
      errDrainer.start();
      outDrainer.start();
      
      startFakeWaitPeriod();
    } catch(IOException ioe) {
      ioe.printStackTrace();
    }
  }
  
  protected void runScript(String scriptName) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    
    File     dir  = new File(m_productDir, "bin");
    String   ext  = Os.isWindows() ? ".bat" : ".sh";
    File     file = new File(dir, scriptName+ext);
    String[] cmd  = {file.getAbsolutePath()};
    
    try {
      Process p = Runtime.getRuntime().exec(cmd);

      InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
      InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream());
      
      errDrainer.start();
      outDrainer.start();

      startFakeWaitPeriod();
    } catch(IOException ioe) {
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
  
  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(getBundleString("quit.action.name"));
    }

    public void actionPerformed(ActionEvent ae) {
      quit();
    }
  }
  
  class AboutAction extends XAbstractAction {
    org.dijon.Container m_aboutPanel;
    
    AboutAction() {
      super(getBundleString("about.title.prefix")+HyperlinkFrame.this.getTitle());
    }

    DictionaryResource loadTopRes() {
      InputStream        is     = getClass().getResourceAsStream("Welcome.xml");
      DictionaryResource topRes = null;
      
      try {
        topRes = ApplicationManager.loadResource(is);
      } catch(Exception e) {
        e.printStackTrace();
      }
      
      return topRes;
    }
    
    public void actionPerformed(ActionEvent ae) {
      if(m_aboutPanel == null) {
        DictionaryResource topRes = loadTopRes();
        
        if(topRes != null) {
          m_aboutPanel = (org.dijon.Container)topRes.resolve("AboutPanel");
          
          ProductInfo prodInfo = ProductInfo.getThisProductInfo();
          Label       label    = (Label)m_aboutPanel.findComponent("TitleLabel");
          
          label.setText(prodInfo.toShortString());

          label = (Label)m_aboutPanel.findComponent("CopyrightLabel");
          label.setText(prodInfo.copyright());
        }
      }

      JOptionPane.showConfirmDialog(HyperlinkFrame.this,
                                    m_aboutPanel,
                                    getName(),
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.PLAIN_MESSAGE);
    }
  }
  
  class PageListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent pce) {
      pack();
      center();
      setVisible(true);
    }
  }
}
