/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.util.ListResourceBundle;

public class AdminClientBundle extends ListResourceBundle {
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
     
    {"service.unavailable",
     "Service Unavailable: {0}"},
     
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
     "Creation count since active server start"},

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
    
    {"dso.cache.miss.rate",
     "Cache Miss Rate"},
    
    {"dso.cache.miss.rate.label",
     "Cache Misses per second"},

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

    {"update-checker.action.label",
     "Update Checker..."},
     
    {"update-checker.connect.failed.msg",
     "Unable to connect to update site."},

    {"update-checker.current.msg",
     "Your software is up-to-date."},
      
    {"update-checker.updates.available.msg",
     "New Terracotta versions are now available."},
       
    {"update-checker.release-notes.label",
     "Release notes"},
        
    {"update-checker.action.title",
     "Terracotta Update Checker"},
     
    {"update-checker.last.checked.msg",
     "Last checked: {0}"},
     
    {"version.check.enable.label",
     "Check Server Version"},

    {"version.check.disable.label",
     "Disable version checks"},
     
    {"version.check.message",
     "<html><h3>Version mismatch for {0}.</h3><br>"
       + "<table border=1 cellspacing=1><tr><td align=right>Terracotta Server Version:</td><td>{1}"
       + "</tr><tr><td align=right>AdminConsole Version:</td><td>{2}"
       + "</td></tr></table><h3>Continue?</h3></html>"
    }
  };
}
