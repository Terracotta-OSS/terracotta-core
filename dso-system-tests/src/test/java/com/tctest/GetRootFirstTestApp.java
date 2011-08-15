/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.awt.Color;

/**
 * Test that an individual VM can get a root before a set method is called on it
 */
public class GetRootFirstTestApp extends AbstractTransparentApp {
  public final int     participantCount = 2;
  private Participant  participant      = new Participant();
  private Color        color;
  private static Color staticColor;

  public GetRootFirstTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    System.out.println("Starting App:" + this.getApplicationId());
    waitForAllParticipants();
    synchronized (participant) {
      if (participant.getStage() == 0) {
        System.out.println("App:" + this.getApplicationId() + " stage:" + participant.getStage() + " color:" + color
                           + " staticColor:" + staticColor);
        Assert.eval(color == null);
        Assert.eval(staticColor == null);
        color = Color.RED;
        staticColor = Color.BLUE;
        participant.incrementStage();
      } else {
        System.out.println("App:" + this.getApplicationId() + " stage:" + participant.getStage() + " color:" + color
                           + " staticColor:" + staticColor);
        Assert.eval(color.equals(Color.RED));
        Assert.eval(staticColor.equals(Color.BLUE));
      }
    }
    System.out.println("Done App:" + this.getApplicationId());
  }

  private void waitForAllParticipants() {
    synchronized (participant) {
      participant.incrementCount();
      while (participant.getCount() < participantCount) {
        try {
          participant.wait();
        } catch (InterruptedException ie) { // ignore
        }
      }
      participant.notifyAll();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GetRootFirstTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String method = "* " + testClass + ".*(..)";
    config.addWriteAutolock(method);

    spec.addRoot("color", "color");
    spec.addRoot("staticColor", "staticColor");
    spec.addRoot("participant", "participant");
    
    config.addIncludePattern(Participant.class.getName());
  }

  private static class Participant {
    private int count = 0;
    private int stage = 0;

    public void incrementStage() {
      stage++;
    }

    public int getStage() {
      return stage;
    }

    public void incrementCount() {
      count++;
    }

    public int getCount() {
      return count;
    }
  }
}
