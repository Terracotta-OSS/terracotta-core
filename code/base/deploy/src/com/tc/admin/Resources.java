/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.util.ListResourceBundle;

public class Resources extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"sessions",
     "Sessions"},
     
    {"title",
     "Terracotta Administrator Console"},

    {"new.server.action.label",
     "New server"},
       
    {"quit.action.label",
     "Quit"},
       
    {"connect.title",
     "Connect to JMX Server"},

    {"connecting.to",
     "Connecting to {0} ..."},

    {"connected.to",
     "Connected to {0}"},

    {"cannot.connect.to",
     "Unable to connect to {0}"},

    {"cannot.connect.to.extended",
     "Unable to connect to {0}: {1}"},
    
    {"unknown.host",
     "Unknown host: {0}"},
       
    {"disconnecting.from",
     "Disconnecting from {0} ..."},

    {"disconnected.from",
     "Disconnected from {0}"},

    {"deleted.server",
     "Deleted {0}"},

    {"server.properties.headings",
     new String[] {"Name", "Value"}},

    {"dso",
     "DSO"},

    {"dso.roots",
     "Roots"},

    {"dso.locks",
     "Locks"},

    {"refresh.name",
     "Refresh"},

    {"dso.roots.refreshing",
     "Refreshing roots..."},

    {"dso.deadlocks.detect",
     "Detect deadlocks"},

    {"dso.deadlocks.detecting",
     "Detecting deadlocks..."},

    {"dso.classes",
     "Classes"},

    {"dso.allClasses",
     "All classes"},

    {"dso.classes.refreshing",
     "Refreshing classes..."},

    {"dso.classes.className",
     "Class"},

    {"dso.classes.instanceCount",
     "Creation count"},

    {"dso.locks.refreshing",
     "Refreshing locks..."},

    {"dso.object.flush.rate",
     "Object Flush Rate"},

    {"dso.object.fault.rate",
     "Object Fault Rate"},

    {"dso.transaction.rate",
     "Transaction Rate"},

    {"dso.root.retrieving",
     "Retrieving new DSO root..."},

    {"dso.root.new",
     "Added new DSO root: "},
    
    {"clients",
     "Clients"},

    {"dso.client.retrieving",
     "Retrieving new DSO client..."},

    {"dso.client.new",
     "Added new DSO client: "},

    {"dso.client.detaching",
     "Detaching DSO client..."},

    {"dso.client.detached",
     "Detached DSO client: "},
    
    {"dso.client.host",
     "Host"},

    {"dso.client.port",
     "Port"},

    {"dso.client.channelID",
     "ChannelID"},

    {"dso.gcstats",
     "Garbage collection"},

    {"map.entry",
     "MapEntry"},

    {"log.error",
     "ERROR"},

    {"log.warn",
     "WARN"},

    {"log.info",
     "INFO"},

    {"dso.cache.rate.domain.label",
     "Time"},

    {"dso.cache.rate.range.label",
     "Objects per second"},

    {"dso.transaction.rate.range.label",
     "Transactions per second"},

    {"dso.cache.activity",
     "Cache activity"},
    
    {"dso.cache.hit.ratio",
     "Cache hit ratio"},
    
    {"dso.cache.hit.ration.range.label",
     "Cache hits/Cache lookups"},

    {"dso.gcstats.iteration",
     "Iteration"},

    {"dso.gcstats.startTime",
     "Start time"},

    {"dso.gcstats.elapsedTime",
     "Elapsed time (ms.)"},

    {"dso.gcstats.beginObjectCount",
     "Begin object count"},

    {"dso.gcstats.candidateGarbageCount",
     "Candidate garbage count"},

    {"dso.gcstats.actualGarbageCount",
     "Actual garbage count"},

    {"dso.all.statistics",
     "All statistics"},

    {"file.menu.label",
     "File"},
     
    {"help.menu.label",
     "Help"},
     
    {"help.item.label",
     "Terracotta Console Help..."},
     
    {"about.action.label",
     "About Terracotta Console"},
  };
}
