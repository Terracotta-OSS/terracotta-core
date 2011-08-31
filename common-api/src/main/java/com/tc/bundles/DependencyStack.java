/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.util.Iterator;
import java.util.Stack;

public class DependencyStack implements Iterable<Object> {

  private final Stack<Object> stack = new Stack();

  public void push(String groupId, String artifactId, String version) {
    StringBuffer buf = new StringBuffer(artifactId);
    buf.append(" version ");
    buf.append(OSGiToMaven.bundleVersionToProjectVersion(version)).append(" (");
    if (groupId.length() > 0) buf.append("group-id: ").append(groupId).append(", ");
    buf.append("file: ").append(OSGiToMaven.makeBundleFilename(artifactId, version, false)).append(")");
    stack.push(buf.toString());
  }

  public void push(String symbolicName, String version) {
    push(OSGiToMaven.groupIdFromSymbolicName(symbolicName), OSGiToMaven.artifactIdFromSymbolicName(symbolicName),
         version);
  }

  public int size() {
    return stack.size();
  }

  public DependencyStack push(DependencyStack pushed) {
    return (DependencyStack) stack.push(pushed);
  }

  public Iterator<Object> iterator() {
    return stack.iterator();
  }
}