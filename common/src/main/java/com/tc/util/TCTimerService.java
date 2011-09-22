/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class TCTimerService {
  private final static TCTimerService INSTANCE = new TCTimerService();
  private final List<Timer>           timers   = new ArrayList<Timer>();

  private TCTimerService() {
    //
  }

  public static TCTimerService getInstance() {
    return INSTANCE;
  }

  public synchronized Timer getTimer(String name, boolean isDaemon) {
    Timer timer = new Timer(name, isDaemon);
    timers.add(timer);
    return timer;
  }

  public synchronized Timer getTimer(String name) {
    return getTimer(name, true);
  }

  public synchronized void shutdown() {
    for (Timer timer : timers) {
      timer.cancel();
    }
  }
}
