/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.apache.commons.io.IOUtils;

import com.tc.logging.JDKLogging;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public abstract class AbstractApplication implements IApplication {
  private final String name;

  static {
    if (!Boolean.getBoolean("javax.management.remote.debug")) {
      JDKLogging.setLevel("javax.management.remote", Level.OFF);
      JDKLogging.setLevel("com.sun.jmx.remote.opt.util", Level.OFF);
    }
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

    if (Os.isMac()) {
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.showGrowBox", "true");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    }
  }

  protected AbstractApplication() {
    this.name = getClass().getSimpleName();
  }

  protected AbstractApplication(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String[] parseArgs(String[] args) {
    return args;
  }

  private File getPrefsFile() {
    return new File(System.getProperty("user.home"), "." + getName() + ".xml");
  }

  public Preferences loadPrefs() {
    FileInputStream fis = null;

    try {
      File f = getPrefsFile();
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
      File f = getPrefsFile();
      fos = new FileOutputStream(f);
      Preferences prefs = getApplicationContext().getPrefs();
      prefs.exportSubtree(fos);
      prefs.flush();
    } catch (IOException e) {
      /**/
    } catch (BackingStoreException e) {
      /**/
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  public abstract void start();
}
