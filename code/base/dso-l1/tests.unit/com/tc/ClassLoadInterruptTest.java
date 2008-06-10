/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.tc.test.TCTestCase;

import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassLoadInterruptTest extends TCTestCase {
  
  public ClassLoadInterruptTest() {
    //disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void testClassLoadInterrupt() throws Exception {
    final URL resource = this.getClass().getClassLoader().getResource("com/sleepycat/je/Database.class");

    final String resourceString = resource.getFile();
    assertTrue(resourceString.startsWith("file:"));
    final int jarIndex = resourceString.indexOf(".jar");
    assertTrue(jarIndex > -1);
    final String jarFileName = resourceString.substring("file:".length(), jarIndex + ".jar".length());

    System.out.println(jarFileName);

    final ZipFile jarFile = new ZipFile(jarFileName);

    final Enumeration jarEnum = jarFile.entries();

    final Thread t1 = new Thread(new Runnable() {
      public void run() {
        // make sure that both threads are started before the interruption begins
        synchronized (ClassLoadInterruptTest.this) {
          ClassLoadInterruptTest.this.notifyAll();

          try {
            ClassLoadInterruptTest.this.wait();
          } catch (InterruptedException e) {
            // do nothing
          }
        }

        // iterate while the other thread is interrupting
        while (jarEnum.hasMoreElements()) {
          ZipEntry jarEntry = ((ZipEntry)jarEnum.nextElement());
          String jarEntryName = jarEntry.getName();

          if (jarEntryName.endsWith(".class")) {
            String className = jarEntryName.substring(0, jarEntryName.length() - ".class".length());
            className = className.replace('/', '.');
            Class klass = null;
            try {
              klass = Class.forName(className);
            } catch (ClassNotFoundException e) {
              continue;
            } catch (NoClassDefFoundError e) {
              fail(e.getMessage());
            }
            System.out.println("> loaded : "+klass);
          }
        }
      }
    });

    final Thread t2 = new Thread(new Runnable() {
      public void run() {
        // make sure that both threads are started before the interruption begins
        synchronized (ClassLoadInterruptTest.this) {
          ClassLoadInterruptTest.this.notifyAll();
        }

        // interrupt the other thread in a tight loop
        while (t1.isAlive()) {
          synchronized (ClassLoadInterruptTest.this) {
            System.out.println("> interrupting thread 1");
            t1.interrupt();
          }
          Thread.yield();
        }
      }
    });

    // start both threads, only starting the 2nd one after the 1st one is alive
    synchronized (this) {
      t1.start();
      while (!t1.isAlive()) {
        Thread.sleep(50);
      }
      this.wait();
    }
    t2.start();

    // wait for both threads to finish
    t1.join();
    t2.join();
  }
}
