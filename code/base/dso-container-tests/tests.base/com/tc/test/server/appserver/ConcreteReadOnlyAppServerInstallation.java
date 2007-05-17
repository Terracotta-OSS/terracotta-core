/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.util.ZipBuilder;
import com.tc.util.runtime.Os;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class manages the installation of the read-only portion of a running appserver. A timestamp is used to cache the
 * installation for future reference. This portion constitutes the majority of an appserver's filesize. This class
 * should not be referenced by any class other than {@link AbstractAppServerInstallation}.
 */
final class ConcreteReadOnlyAppServerInstallation {

  // private static final DateFormat df = new SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
  // "11/04/07 6:38:56 PDT PM"
  private static final String FORMAT = "dd/MM/yy H:mm:ss zzz a";
  

  static File create(URL host, File serverDir, String serverType, String majorVersion, String minorVersion)
      throws Exception {
    File serverInstallDir = new File(serverDir + File.separator + serverType + "-" + majorVersion + "." + minorVersion
                                     + "-install");
    serverInstallDir.mkdir();
    File timestampFile = new File(serverInstallDir + File.separator + "timestamp");
    if (equalTimestamps(host, serverType, majorVersion, minorVersion, timestampFile)) { return serverInstallDir; }
    File deleteServer = new File(serverInstallDir + File.separator + serverType + "-" + majorVersion + "."
                                 + minorVersion);
    if (deleteServer.exists()) {
      deleteServer.delete();
    }
    BufferedInputStream in = new BufferedInputStream(appendPath(host, serverType, majorVersion, minorVersion)
        .openStream());
    ZipBuilder.unzip(in, serverInstallDir);
    in.close();
    return serverInstallDir;
  }

  private static boolean equalTimestamps(URL host, String serverType, String majorVersion, String minorVersion,
                                         File timestampFile) throws Exception {
    URLConnection conn = appendPath(host, serverType, majorVersion, minorVersion).openConnection();
    System.out.println("**Connection=" + host);
    System.out.println("**Connection=" + conn);

    long modified = conn.getLastModified();
    DateFormat df = new SimpleDateFormat(FORMAT, Locale.US);
    String modifiedTimestamp = df.format(new Date(modified));
    // temporarily modify timestamp
    // TODO: remove below line
    writeTimestamp(modifiedTimestamp, timestampFile);
    
    if (!timestampFile.exists()) {
      writeTimestamp(modifiedTimestamp, timestampFile);
      return false;
    }
    
    LineNumberReader in = new LineNumberReader(new FileReader(timestampFile));
    String serverStamp = in.readLine();
    in.close();
    if (df.parse(serverStamp).getTime() != df.parse(modifiedTimestamp).getTime()) {
      writeTimestamp(modifiedTimestamp, timestampFile);
      return false;
    }
    return true;
  }

  private static void writeTimestamp(String modifiedTimestamp, File timestampFile) throws IOException {
    PrintWriter out = new PrintWriter(new FileWriter(timestampFile));
    out.println(modifiedTimestamp);
    out.close();
  }

  // return platform the test running on
  private static String resolvePlatform() {
    return Os.platform();
  }

  private static URL appendPath(URL host, String serverType, String majorVersion, String minorVersion) throws Exception {
    String baseUrl = host.toExternalForm();
    String appendedPath = serverType + "/" + resolvePlatform() + "/" + serverType.toLowerCase() + "-"
                          + majorVersion.toLowerCase() + "." + minorVersion.toLowerCase() + ".zip";
    return new URL(baseUrl + "/" + appendedPath);
  }
}
