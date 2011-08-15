/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.SwingUtilities;

public class StreamReader extends Thread {
  TextPaneUpdater      updater;
  InputStreamReader    streamReader;
  BufferedReader       bufferedReader;
  volatile boolean     stop;
  OutputStreamListener listener;
  String               trigger;

  public StreamReader(InputStream stream, OutputStreamListener listener, String trigger) {
    this(stream, null, listener, trigger);
  }

  public StreamReader(InputStream stream, TextPaneUpdater updater, OutputStreamListener listener, String trigger) {
    this.updater = updater;
    this.listener = listener;
    this.trigger = trigger;
    streamReader = new InputStreamReader(stream);
    bufferedReader = new BufferedReader(streamReader);
    stop = false;
  }

  public void setTriggerListener(OutputStreamListener listener) {
    this.listener = listener;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public void run() {
    String line;

    while (!stop) {
      try {
        if ((line = bufferedReader.readLine()) == null) {
          IOUtils.closeQuietly(bufferedReader);
          return;
        } else {
          if (updater != null) {
            update(line);
          }
          if (listener != null && trigger != null && StringUtils.contains(line, trigger)) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                listener.triggerEncountered();
              }
            });
          }
          continue;
        }
      } catch (IOException ioe) {
        IOUtils.closeQuietly(bufferedReader);
        return;
      }
    }

    IOUtils.closeQuietly(bufferedReader);
  }

  private void update(String line) {
    try {
      updater.setLine(line);
      SwingUtilities.invokeAndWait(updater);
    } catch (Exception e) {/**/
    }
  }

  public synchronized void finish() {
    stop = true;
    IOUtils.closeQuietly(bufferedReader);
  }
}
