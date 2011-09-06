/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.sysinfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public final class EnvStats {

  public static final String filename = "tc-envstats.txt";
    
  public static void writeReport(File directory) throws IOException, IllegalArgumentException {
    if (!directory.isDirectory()) throw new IllegalArgumentException(directory + " is not a directory");
    FileOutputStream out = new FileOutputStream(directory + File.separator + filename);
    out.write(report().getBytes());
    out.flush();
    out.close();
  }
  
  public static String report() {
    StringBuffer sb = new StringBuffer();
    sb.append("***** Terracotta System Environment Report *****\n\n");
    sb.append("Date Created: " + DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())) + "\n\n");
    sb.append("java.runtime.name=" + System.getProperty("java.runtime.name") + "\n");
    sb.append("java.vm.version=" + System.getProperty("java.vm.version") + "\n");
    sb.append("java.vm.vendor=" + System.getProperty("java.vm.vendor") + "\n");
    sb.append("java.vendor.url=" + System.getProperty("java.vendor.url") + "\n");
    sb.append("java.vm.name=" + System.getProperty("java.vm.name") + "\n");
    sb.append("file.encoding.pkg=" + System.getProperty("file.encoding.pkg") + "\n");
    sb.append("user.country=" + System.getProperty("user.country") + "\n");
    sb.append("sun.os.patch.level=" + System.getProperty("sun.os.patch.level") + "\n");
    sb.append("java.vm.specification.name=" + System.getProperty("java.vm.specification.name") + "\n");
    sb.append("java.runtime.version=" + System.getProperty("java.runtime.version") + "\n");
    sb.append("java.awt.graphicsenv=" + System.getProperty("java.awt.graphicsenv") + "\n");
    sb.append("os.arch=" + System.getProperty("os.arch") + "\n");
    sb.append("java.vm.specification.vendor=" + System.getProperty("java.vm.specification.vendor") + "\n");
    sb.append("os.name=" + System.getProperty("os.name") + "\n");
    sb.append("java.library.path=" + System.getProperty("java.library.path") + "\n");
    sb.append("java.specification.name=" + System.getProperty("java.specification.name") + "\n");
    sb.append("java.class.version=" + System.getProperty("java.class.version") + "\n");
    sb.append("java.util.prefs.PreferencesFactory=" + System.getProperty("java.util.prefs.PreferencesFactory") + "\n");
    sb.append("os.version=" + System.getProperty("os.version") + "\n");
    sb.append("user.timezone=" + System.getProperty("user.timezone") + "\n");
    sb.append("java.awt.printerjob=" + System.getProperty("java.awt.printerjob") + "\n");
    sb.append("file.encoding=" + System.getProperty("file.encoding") + "\n");
    sb.append("java.specification.version=" + System.getProperty("java.specification.version") + "\n");
    sb.append("java.class.path=" + System.getProperty("java.class.path") + "\n");
    sb.append("java.vm.specification.version=" + System.getProperty("java.vm.specification.version") + "\n");
    sb.append("sun.arch.data.model=" + System.getProperty("sun.arch.data.model") + "\n");
    sb.append("java.home=" + System.getProperty("java.home") + "\n");
    sb.append("java.specification.vendor=" + System.getProperty("java.specification.vendor") + "\n");
    sb.append("user.language=" + System.getProperty("user.language") + "\n");
    sb.append("java.vm.info=" + System.getProperty("java.vm.info") + "\n");
    sb.append("java.version=" + System.getProperty("java.version") + "\n");
    sb.append("java.ext.dirs=" + System.getProperty("java.ext.dirs") + "\n");
    sb.append("sun.boot.class.path=" + System.getProperty("sun.boot.class.path") + "\n");
    sb.append("java.vendor=" + System.getProperty("java.vendor") + "\n");
    sb.append("java.vendor.url.bug=" + System.getProperty("java.vendor.url.bug") + "\n");
    sb.append("sun.cpu.endian=" + System.getProperty("sun.cpu.endian") + "\n");
    sb.append("sun.io.unicode.encoding=" + System.getProperty("sun.io.unicode.encoding") + "\n");
    sb.append("sun.cpu.isalist=" + System.getProperty("sun.cpu.isalist") + "\n");
    return sb.toString();
  }
}
