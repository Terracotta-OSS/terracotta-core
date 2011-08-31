/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class Splash extends JDialog {
  public Splash(String title) {
    super();

    setTitle(title);
    JLabel label = new JLabel(title);
    label.setFont(UIManager.getFont("InternalFrame.titleFont"));
    label.setVerticalTextPosition(SwingConstants.TOP);
    label.setHorizontalTextPosition(SwingConstants.CENTER);
    label.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/logo.png")));
    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Container contentPane = getContentPane();
    ((JComponent) contentPane).setBorder(UIManager.getBorder("InternalFrame.border"));
    contentPane.setLayout(new BorderLayout());
    contentPane.add(label, BorderLayout.CENTER);
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    JPanel progressPanel = new JPanel(new BorderLayout());
    progressPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
    progressPanel.add(progressBar);
    contentPane.add(progressPanel, BorderLayout.SOUTH);
    setUndecorated(true);
    pack();
    WindowHelper.center(this);
  }

  private static File getJavaCmd() {
    File javaBin = new File(System.getProperty("java.home"), "bin");
    return new File(javaBin, "java" + (Os.isWindows() ? ".exe" : ""));
  }

  public static Process start(final String title, final Runnable callback) throws IOException {
    String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-cp", System.getProperty("java.class.path"),
        Splash.class.getName(), title };

    final Process p = Runtime.getRuntime().exec(cmdarray);
    InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream());
    StreamReader outReader = new StreamReader(p.getInputStream(), new OutputStreamListener() {
      public void triggerEncountered() {
        Thread shutdownHook = new Thread() {
          public void run() {
            p.destroy();
          }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        callback.run();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      }
    }, "GO");

    errDrainer.start();
    outReader.start();

    return p;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {/**/
    }

    Splash splash = new Splash(args[0]);
    splash.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        System.out.println("GO");
      }
    });
    splash.setVisible(true);
  }
}
