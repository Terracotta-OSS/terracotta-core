/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;

public class Debug {

  public static void sendTestDetails(String testName) {
    try {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);

      pw.println(new Date());
      String host = InetAddress.getLocalHost().getHostName();
      pw.println();

      String[] props = (String[]) System.getProperties().keySet().toArray(new String[] {});
      Arrays.sort(props);
      for (int i = 0; i < props.length; i++) {
        pw.println(props[i] + " --> " + System.getProperty(props[i], "null"));
      }

      pw.flush();

      mail("execution: (" + host + ") " + testName, sw.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void mail(String subject, String content) throws Exception {
    String host = "mail.terracottatech.com";
    String to = "Tim Eck <teck@terracotta.org>";
    String from = "Tim Eck <teck@terracotta.org>";

    Socket s = new Socket(host, 25);

    try {
      OutputStream out = s.getOutputStream();
      InputStream in = s.getInputStream();

      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      PrintWriter pw = new PrintWriter(out);

      readLine(br);
      writeLine(pw, "HELO " + InetAddress.getLocalHost().getHostName());
      readLine(br);
      writeLine(pw, "MAIL FROM:teck@terracotta.org");
      readLine(br);
      writeLine(pw, "RCPT TO:teck@terracotta.org");
      readLine(br);
      writeLine(pw, "DATA");
      readLine(br);
      writeLine(pw, "Subject: " + subject);
      writeLine(pw, "To: " + to);
      writeLine(pw, "From: " + from);
      writeLine(pw, "");
      writeLine(pw, content);
      writeLine(pw, ".");
      readLine(br);
      writeLine(pw, "QUIT");
      readLine(br);
    } finally {
      try {
        if (!s.isClosed()) {
          s.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void writeLine(PrintWriter pw, String line) {
    pw.println(line);
    pw.flush();
  }

  private static void readLine(BufferedReader br) throws IOException {
    String line = br.readLine();
    System.err.println(line);
  }
}
