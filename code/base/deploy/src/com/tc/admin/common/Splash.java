/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Container;
import org.dijon.Dialog;
import org.dijon.Label;

import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class Splash extends Dialog {
  public Splash(String title) {
    super(title);
    
    Label label = new Label(title);
    label.setFont(UIManager.getFont("InternalFrame.titleFont"));
    label.setVerticalTextPosition(SwingConstants.TOP);
    label.setHorizontalTextPosition(SwingConstants.CENTER);
    label.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/logo.gif")));
    Container contentPane = (Container)getContentPane(); 
    contentPane.setBorder(BorderFactory.createEtchedBorder());
    contentPane.setLayout(new BorderLayout());
    contentPane.add(label, BorderLayout.CENTER);
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    JPanel progressPanel = new JPanel(new BorderLayout());
    progressPanel.setBorder(new EmptyBorder(2,2,2,2));
    progressPanel.add(progressBar);
    contentPane.add(progressPanel, BorderLayout.SOUTH);
    setUndecorated(true);
    pack();
    center();
  }

  private static File getJavaCmd() {
    File javaBin = new File(System.getProperty("java.home"), "bin");
    return new File(javaBin, "java" + (Os.isWindows() ? ".exe" : ""));
  }

  public static Process start(final String title, final Runnable callback) throws IOException {
    String[] cmdarray = {
      getJavaCmd().getAbsolutePath(),
      "-cp", System.getProperty("java.class.path"),
      Splash.class.getName(),
      title
    };

    Process p = Runtime.getRuntime().exec(cmdarray);
    InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
    StreamReader outReader = new StreamReader(
      p.getInputStream(),
      new OutputStreamListener() {
        public void triggerEncountered() {
          callback.run();
        }
      },
      "GO");

    errDrainer.start();
    outReader.start();
    
    return p;
  }
  
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e) {/**/}
    
    Splash splash = new Splash(args[0]);
    splash.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        System.out.println("GO");
      }
    });
    splash.setVisible(true);
  }
}
