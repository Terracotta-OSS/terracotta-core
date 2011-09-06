/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli.commands;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;

public class CommandRetrieveStatistics extends AbstractCliCommand {
  static {
    Appender appender = new ConsoleAppender(new SimpleLayout());
    Logger httpclient_logger1 = Logger.getLogger("org.apache.commons.httpclient");
    httpclient_logger1.addAppender(appender);
    httpclient_logger1.setLevel(Level.WARN);
    Logger httpclient_logger2 = Logger.getLogger("httpclient");
    httpclient_logger2.addAppender(appender);
    httpclient_logger2.setLevel(Level.WARN);
  }

  private final static String[] ARGUMENT_NAMES = new String[] { "filename" };

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);

    final File file = new File(arguments[0]);
    if (file.exists()) {
      System.out.println("> Aborted statistics retrieval since the target file '" + file.getAbsolutePath()
                         + "' already exists.");
      return;
    }

    GetMethod get = null;
    try {
      // prepare the URL to retrieve the data from
      String uri = getStatsExportServletURI(connection);
      URL url = new URL(uri);
      HttpClient httpClient = new HttpClient();

      // connect to the URL
      get = new GetMethod(url.toString());
      get.setFollowRedirects(true);
      int status = httpClient.executeMethod(get);
      if (status != HttpStatus.SC_OK) {
        System.out.println("> The http client has encountered a status code other than ok for the url: " + url
                           + " status: " + HttpStatus.getStatusText(status));
        return;
      }

      // retrieve all the data from the request and stream it to the file
      StreamCopierRunnable runnable = new StreamCopierRunnable(get, file);
      Thread t = new Thread(runnable);
      synchronized (runnable) {
        System.out.println("> Downloading statistics to '" + file.getAbsolutePath() + "'");
        t.start();

        // wait for the download thread to finish
        while (t.isAlive()) {
          System.out.print(".");
          runnable.wait(500);
        }
        System.out.println();
        System.out.println("Done");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (get != null) {
        get.releaseConnection();
      }
    }
  }

  private String getStatsExportServletURI(final GathererConnection connection) throws Exception {
    Integer dsoPort = Integer.valueOf(connection.getDSOListenPort());
    Object[] args = new Object[] { connection.getHost(), dsoPort.toString() };
    return MessageFormat.format("http://{0}:{1}/statistics-gatherer/retrieveStatistics", args);
  }

  private static class StreamCopierRunnable implements Runnable {
    final private GetMethod getMethod;
    final private File      outFile;

    StreamCopierRunnable(final GetMethod getMethod, final File outFile) {
      this.getMethod = getMethod;
      this.outFile = outFile;
    }

    public void run() {
      FileOutputStream out = null;

      try {
        out = new FileOutputStream(outFile);
        InputStream in = getMethod.getResponseBodyAsStream();

        byte[] buffer = new byte[1024 * 8];
        int count;
        try {
          while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
          }
        } finally {
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        IOUtils.closeQuietly(out);
        getMethod.releaseConnection();

        synchronized (this) {
          this.notifyAll();
        }
      }
    }
  }
}