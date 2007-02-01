/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import org.dijon.ApplicationManager;
import org.dijon.Frame;
import org.dijon.ScrollPane;
import org.dijon.TextPane;

import com.tc.admin.TCStop;
import com.tc.admin.common.StreamReader;
import com.tc.admin.common.TextPaneUpdater;
import com.tc.admin.common.XTextPane;
import com.tc.util.BundleHelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;

public class DSOSamplesFrame extends HyperlinkFrame {
  private BundleHelper m_bundleHelper = new BundleHelper(DSOSamplesFrame.class);
  private TextPane     m_outputPane;
  private ArrayList    m_processList;

  public DSOSamplesFrame() {
    super();

    setTitle(m_bundleHelper.getString("frame.title"));

    m_processList = new ArrayList();
    m_outputPane = new XTextPane();
    m_outputPane.setForeground(Color.GREEN);
    m_outputPane.setBackground(Color.BLACK);
    ScrollPane scroller = new ScrollPane(m_outputPane);
    scroller.setPreferredSize(new Dimension(500, 200));
    getContentPane().add(scroller, BorderLayout.SOUTH);
    runServer();
  }

  private void toOutputPane(String s) {
    Document doc = m_outputPane.getDocument();

    try {
      doc.insertString(doc.getLength(), s+"\n", null);
    } catch(Exception e) {/**/}
  }
  
  protected void quit() {
    stopProcesses();
  }
  
  protected URL getPage() {
    return getClass().getResource("SamplesPojos.html");
  }
  
  private StreamReader createStreamReader(InputStream stream) {
    return new StreamReader(stream, new TextPaneUpdater(m_outputPane), null, null);
  }
  
  private StreamReader createStreamReader(InputStream stream, TextPane textPane) {
    return new StreamReader(stream, new TextPaneUpdater(textPane), null, null);
  }

  private void stopProcesses() {
    String host = "localhost";
    int    port = 9520;
    
    try {
      new TCStop(host, port).stop();
    } catch(Exception e) {
      toOutputPane(e.getMessage());
    }
  
    Iterator iter = m_processList.iterator();
    while(iter.hasNext()) {
      Process p = (Process)iter.next();
      
      try {
        p.exitValue();
      } catch(Exception e) {
        p.destroy();
      }
    }
    
    new Thread() {
      public void run() {
        TCStop tester = new TCStop("localhost", 9520);
        
        while(true) {
          try {
            sleep(2000);
            tester.stop();
          } catch(Exception e) {
            System.exit(0);
          }
        }
      }
    }.start();
  }
  
  private void runServer() {
    try {
      String[] cmdarray = {getJavaCmd().getAbsolutePath(),
                           "-Dtc.config=tc-config.xml", 
                           "-Dtc.install-root="+System.getProperty("tc.install-root"),
                           "-cp", getTCLib().getAbsolutePath(),
                           "com.tc.server.TCServerMain"};

      Process      p          = exec(cmdarray, null, getSamplesDir());
      StreamReader errDrainer = createStreamReader(p.getErrorStream());
      StreamReader outDrainer = createStreamReader(p.getInputStream());
      
      errDrainer.start();
      outDrainer.start();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void runSample(String dirName, String className) {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String   bootPath = getBootPath();
      String[] cmdarray = {getJavaCmd().getAbsolutePath(),
                           "-Dtc.config=tc-config.xml", 
                           "-Djava.awt.Window.locationByPlatform=true",
                           "-Dtc.install-root="+System.getProperty("tc.install-root"),
                           "-Xbootclasspath/p:"+bootPath,
                           "-cp", "classes",
                           className};

      Process      p          = exec(cmdarray, null, new File(getProductDirectory(), dirName));
      StreamReader errDrainer = createStreamReader(p.getErrorStream());
      StreamReader outDrainer = createStreamReader(p.getInputStream());
      
      errDrainer.start();
      outDrainer.start();
      
      m_processList.add(p);
      startFakeWaitPeriod();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  private void runCoordinationSample() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String   bootPath  = getBootPath();
      File     dir       = new File(getProductDirectory(), "coordination");
      File     lib       = new File(dir, "lib");
      File     oswego    = new File(lib, "concurrent-1.3.4.jar");
      String   classpath = "classes"+System.getProperty("path.separator")+oswego.getAbsolutePath();
      String[] cmdarray  = {getJavaCmd().getAbsolutePath(),
                            "-Dtc.config=tc-config.xml", 
                            "-Djava.awt.Window.locationByPlatform=true",
                            "-Dtc.install-root="+System.getProperty("tc.install-root"),
                            "-Xbootclasspath/p:"+bootPath,
                            "-cp", classpath,
                            "demo.coordination.Main"};

      final Process p          = exec(cmdarray, null, dir);
      XTextPane     textPane   = new XTextPane();
      StreamReader  errDrainer = createStreamReader(p.getErrorStream(), textPane);
      StreamReader  outDrainer = createStreamReader(p.getInputStream(), textPane);
 
      errDrainer.start();
      outDrainer.start();
      
      m_processList.add(p);
      startFakeWaitPeriod();
      
      Frame frame = new SampleFrame(this, m_bundleHelper.getString("jvm.coordination"));
      frame.getContentPane().add(new ScrollPane(textPane));
      frame.setSize(new Dimension(500, 300));
      frame.setVisible(true);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          try {
            p.destroy();
          } catch(Exception e) {/**/}
          m_processList.remove(p);
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void runSharedQueueSample() {
    setTextPaneCursor(Cursor.WAIT_CURSOR);
    try {
      String   bootPath  = getBootPath();
      File     dir       = new File(getProductDirectory(), "sharedqueue");
      File     lib       = new File(dir, "lib");
      File     servlet   = new File(lib, "javax.servlet.jar");
      File     jetty     = new File(lib, "org.mortbay.jetty-4.2.20.jar");
      String   pathSep   = System.getProperty("path.separator");
      String   classpath = "classes"+pathSep+servlet.getAbsolutePath()+pathSep+jetty;
      String[] cmdarray  = {getJavaCmd().getAbsolutePath(),
                            "-Dtc.config=tc-config.xml", 
                            "-Djava.awt.Window.locationByPlatform=true",
                            "-Dtc.install-root="+System.getProperty("tc.install-root"),
                            "-Xbootclasspath/p:"+bootPath,
                            "-cp", classpath,
                            "demo.sharedqueue.Main"};

      final Process p          = exec(cmdarray, null, dir);
      XTextPane     textPane   = new XTextPane();
      StreamReader  errDrainer = createStreamReader(p.getErrorStream(), textPane);
      StreamReader  outDrainer = createStreamReader(p.getInputStream(), textPane);
      
      errDrainer.start();
      outDrainer.start();
      
      m_processList.add(p);
      startFakeWaitPeriod();

      Frame frame = new SampleFrame(this, m_bundleHelper.getString("shared.work.queue"));
      frame.getContentPane().add(new ScrollPane(textPane));
      frame.setSize(new Dimension(500, 300));
      frame.setVisible(true);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          try {
            p.destroy();
          } catch(Exception e) {/**/}
          m_processList.remove(p);
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  protected void hyperlinkActivated(AttributeSet anchor, String action) {
    if(action.equals("run_jtable")) {
      toOutputPane(m_bundleHelper.getString("starting.jtable"));
      runSample("jtable", "demo.jtable.Main");
    }
    else if(action.equals("run_sharededitor")) {
      toOutputPane(m_bundleHelper.getString("starting.shared.editor"));
      runSample("sharededitor", "demo.sharededitor.Main");
    }
    else if(action.equals("run_chatter")) {
      toOutputPane(m_bundleHelper.getString("starting.chatter"));
      runSample("chatter", "demo.chatter.Main");
    }
    else if(action.equals("run_coordination")) {
      toOutputPane(m_bundleHelper.getString("starting.jvm.coordination"));
      runCoordinationSample();
    }
    else if(action.equals("run_sharedqueue")) {
      toOutputPane(m_bundleHelper.getString("starting.shared.queue"));
      runSharedQueueSample();
    }
    else if(action.equals("view_readme")) {
      String name = (String)anchor.getAttribute(HTML.Attribute.NAME);
      File   dir  = new File(getProductDirectory(), name);
      File   file = new File(dir, "readme.html");
      
      openURL("file://" + file.getAbsolutePath());
    }
    else if(action.equals("browse_source")) {
      String name       = (String)anchor.getAttribute(HTML.Attribute.NAME);
      File   sampleDir  = new File(getProductDirectory(), name);
      File   sampleDocs = new File(sampleDir, "docs");
      File   index      = new File(sampleDocs, "source.html");
      
      openURL("file://" + index.getAbsolutePath());
    }
    else {
      openURL(action);
    }
  }
  
  public static void main(String[] args) {
    ApplicationManager.parseLAFArgs(args);
    new DSOSamplesFrame();
  }
}

class SampleFrame extends Frame {
  public SampleFrame(Frame parentFrame, String title) {
    super(title);
    getContentPane().setLayout(new BorderLayout());
    setParentFrame(parentFrame);
  }
  
  public Rectangle getPreferredBounds() {return null;}
  public Integer getPreferredState() {return null;}
  public void storeBounds() {/**/}
  public void storeState() {/**/}
}