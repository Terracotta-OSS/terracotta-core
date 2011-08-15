/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class tracks actions performed by a command during execution.  Mostly useful for 
 * testing but possibly also for logging or some other purpose later.
 */
public class ActionLog {

  public interface Action {
    // nothing for sure
  }
  
  public static class InstallAction implements Action {
    public final String groupId; 
    public final String artifactId;
    public final String version;
    public final String installPath;
    
    public InstallAction(String groupId, String artifactId, String version, String installPath) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.installPath = installPath;
    }
  }
  
  public static class ModifyModuleVersionAction implements Action {
    public final String groupId; 
    public final String artifactId;
    public final String oldVersion;
    public final String newVersion;

    public ModifyModuleVersionAction(String groupId, String artifactId, String oldVersion, String newVersion) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.oldVersion = oldVersion;
      this.newVersion = newVersion;
    }
  }
  
  private final List<ActionLog.Action> actions = Collections.synchronizedList(new ArrayList<ActionLog.Action>());
  
  public void addAction(Action action) {
    this.actions.add(action);
  }
  
  public void addInstallAction(String groupId, String artifactId, String version, String absolutePath) {
    InstallAction installAction = new InstallAction(groupId, artifactId, version, absolutePath);
    actions.add(installAction);
  }

  public void addModifiedModuleAction(String groupId, String artifactId, String oldVersion, String newVersion) {
    ModifyModuleVersionAction modifyAction = new ModifyModuleVersionAction(groupId, artifactId, oldVersion, newVersion);
    actions.add(modifyAction);
  }
  
  public Iterator<Action> getActions() {
    return this.actions.iterator();
  }
  
  public int getInstalledCount() {
    int count = 0;
    synchronized(actions) {
      for(Action action : actions) {
        if(action instanceof InstallAction) {
          count++;
        }
      }
    }
    return count;
  }
  
  public boolean installed(String groupId, String artifactId, String version) {
    synchronized(actions) {
      for(Action action : actions) {
        if(action instanceof InstallAction) {
          InstallAction installAction = (InstallAction) action;
          if(installAction.groupId.equals(groupId) && installAction.artifactId.equals(artifactId) && installAction.version.equals(version)) {
            return true;
          }
        }
      }
    }    
    return false;
  }

  public int getActionCount() {
    return actions.size();
  }

  public boolean updatedModuleVersion(String groupId, String artifactId, String newVersion) {
    synchronized(actions) {
      for(Action action : actions) {
        if(action instanceof ModifyModuleVersionAction) {
          ModifyModuleVersionAction modifyAction = (ModifyModuleVersionAction) action;
          if(modifyAction.groupId.equals(groupId) && modifyAction.artifactId.equals(artifactId) && modifyAction.newVersion.equals(newVersion)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
