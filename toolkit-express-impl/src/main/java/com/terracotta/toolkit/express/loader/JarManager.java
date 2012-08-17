/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class JarManager {

  private static final long      DEFAULT_IDLE_TIME = 10 * 1000L;
  private static final long      IDLE_TIME;

  static {
    long idle = DEFAULT_IDLE_TIME;
    String prop = System.getProperty(JarManager.class.getName() + ".idle");
    if (prop != null) {
      idle = Long.valueOf(prop);

      // some sanity checking
      if (idle < 100L) {
        idle = 100L;
      }
    }

    IDLE_TIME = idle;
  }

  private final Map<String, Jar> jars              = new LinkedHashMap<String, Jar>();
  private final long             idleTime;
  private Thread                 idleThread;

  public JarManager() {
    this(IDLE_TIME);
  }

  public JarManager(long idleTime) {
    this.idleTime = idleTime;
  }

  public synchronized Jar getOrCreate(String key, URL source) {
    if (source == null) throw new NullPointerException("null source");
    if (key == null) throw new NullPointerException("key source");

    Jar jar = jars.get(key);
    if (jar == null) {
      jar = new Jar(source, this);
      jars.put(key, jar);
    }
    return jar;
  }

  public synchronized Jar get(String key) {
    if (key == null) throw new NullPointerException("null source");

    return jars.get(key);
  }

  synchronized void jarOpened(Jar jar) {
    if (idleThread == null) {
      idleThread = new IdleThread(this, idleTime);
      idleThread.start();
    }
  }

  private synchronized Collection<Jar> getJarsSnapshot() {
    return new ArrayList<Jar>(jars.values());
  }

  private boolean reapIdleJars() {
    // the locking logic is here is a litle complicated in order to avoid deadlocks
    // Since Jar can call on this.jarOpened() while holding its own lock we need to make sure not to do the same locking
    // in reverse. To accomplish that we make sure we take all the Jar locks first and only then take our own lock

    boolean anyOpen = false;
    boolean reaped = false;

    while (!reaped) {
      Collection<Jar> snapshot = getJarsSnapshot();

      for (Jar jar : snapshot) {
        jar.lock();
      }

      synchronized (this) {
        try {
          // make sure the set of jars hasn't changed. If it has we have to try retaking all the Jar locks again
          if (snapshot.equals(getJarsSnapshot())) {
            reaped = true;

            for (Jar jar : jars.values()) {
              if (!jar.deflateIfIdle(idleTime)) {
                anyOpen = true;
              }
            }

            if (!anyOpen) {
              idleThread = null;
            }
          }
        } finally {
          for (Jar jar : snapshot) {
            jar.unlock();
          }
        }
      }
    }

    return !anyOpen;
  }

  private static class IdleThread extends Thread {
    private final long       idle;
    private final JarManager manager;

    IdleThread(JarManager manager, long idle) {
      this.manager = manager;
      this.idle = idle;
      setName("JarManager idle thread");
      setDaemon(true);
    }

    @Override
    public void run() {
      final long sleep = Math.max(1000L, idle / 10);
      while (!manager.reapIdleJars()) {
        try {
          sleep(sleep);
        } catch (InterruptedException e) {
          //
        }
      }
    }
  }

}
