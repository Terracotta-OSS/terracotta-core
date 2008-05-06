/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.deployer;

import java.util.Set;
import java.util.HashSet;

import com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.MatchingJoinPointInfo;

/**
 * Represents a change set of changes to be made to the class graph.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public final class ChangeSet {
  private final Set m_set = new HashSet();

  /**
   * Adds a change set element.
   *
   * @param element
   */
  public void addElement(final Element element) {
    m_set.add(element);
  }

  /**
   * Returns all elements in the change set.
   *
   * @return all elements in the change set
   */
  public Set getElements() {
    return m_set;
  }

  /**
   * Represents a change to be made to the class graph.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
   */
  public static class Element {
    private final CompilationInfo m_compilationInfo;
    private final MatchingJoinPointInfo m_joinPointInfo;

    public Element(final CompilationInfo compilationInfo, final MatchingJoinPointInfo joinPointInfo) {
      m_compilationInfo = compilationInfo;
      m_joinPointInfo = joinPointInfo;
    }

    public CompilationInfo getCompilationInfo() {
      return m_compilationInfo;
    }

    public MatchingJoinPointInfo getJoinPointInfo() {
      return m_joinPointInfo;
    }
  }
}