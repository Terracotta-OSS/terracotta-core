/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Identifies the ClassLoader used to load a particular clustered class.
 * The actual ClassLoader identified by a LoaderDescription may differ
 * from node to node depending on what loaders are available on that node.
 * This is an immutable class.
 */
public final class LoaderDescription {
  
  private static final String APP_GROUP_DELIMITER = "%%";
  private final String name;
  private final String appGroup;
  
  /**
   * @param appGroup the name of a set of applications which can share the same loader,
   * or null if the loader is not allowed to be shared. The empty string is equivalent to null.
   * @param name the classloader description (typically from {@link NamedClassLoader}). 
   * Must be non-empty.
   */
  public LoaderDescription(String appGroup, String name) {
    if (name == null || name.length() < 1) {
      throw new IllegalArgumentException("LoaderDescription name must not be empty");
    }
    this.name = name;
    if (appGroup != null && appGroup.length() == 0) {
      this.appGroup = null;
    } else {
      this.appGroup = appGroup;
    }
  }
  
  public String name() {
    return name;
  }
  
  public String appGroup() {
    return appGroup;
  }
  
  /**
   * @return a string including the appGroup and name tokens. The delimiter
   * used to separate them may also occur within either token, so this
   * string should not be parsed to recover the individual tokens.
   */
  public String toString() {
    return (appGroup == null ? "" : appGroup) + APP_GROUP_DELIMITER + name;
  }
  
  /** will be removed when development is complete */
  public String toDelimitedString() {
    return toString();
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((appGroup == null) ? 0 : appGroup.hashCode());
    result = prime * result + name.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LoaderDescription other = (LoaderDescription) obj;
    if (appGroup == null) {
      if (other.appGroup != null) return false;
    } else if (!appGroup.equals(other.appGroup)) return false;
    if (!name.equals(other.name)) return false;
    return true;
  }

  /**
   * @deprecated this will be removed as soon as development is complete.
   */
  public static LoaderDescription fromString(String desc) {
    int delim = desc.indexOf(APP_GROUP_DELIMITER);
    if (delim < 0) {
      return new LoaderDescription(null, desc);
    } else {
      String appGroup = null;
      if (delim > 0) {
        appGroup = desc.substring(0, delim);
      }
      String name = desc.substring(delim + 2);
      return new LoaderDescription(appGroup, name);
    }
  }
}
