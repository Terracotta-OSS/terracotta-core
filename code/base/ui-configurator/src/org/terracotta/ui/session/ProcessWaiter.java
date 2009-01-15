/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.InputStreamDrainer;
import com.tc.util.concurrent.ThreadUtil;

public class ProcessWaiter extends Thread {
  private Process            process;
  private Runnable           waiter;
  private InputStreamDrainer outReader;
  private InputStreamDrainer errReader;

  public ProcessWaiter(Process process, Runnable waiter) {
    super();

    if (process == null) throw new IllegalArgumentException("process is null");

    this.process = process;
    this.waiter = waiter;
  }

  public ProcessWaiter(Process process) {
    this(process, null);
  }

  public void start(Runnable theWaiter) {
    this.waiter = theWaiter;
    start();
  }

  public void run() {
    outReader = new InputStreamDrainer(process.getInputStream());
    errReader = new InputStreamDrainer(process.getErrorStream());

    outReader.start();
    errReader.start();

    while (true) {
      try {
        if (outReader.isAlive()) {
          outReader.join();
        }
        if (errReader.isAlive()) {
          errReader.join();
        }
        break;
      } catch (InterruptedException ie) {/**/
      }
      ThreadUtil.reallySleep(1000);
    }

    if (waiter != null) {
      waiter.run();
    }
  }

  public Process getProcess() {
    return process;

  }

  public String getOutputBuffer() {
    return outReader.getBufferContent();
  }

  public String getErrorBuffer() {
    return errReader != null ? errReader.getBufferContent() : null;
  }
}
