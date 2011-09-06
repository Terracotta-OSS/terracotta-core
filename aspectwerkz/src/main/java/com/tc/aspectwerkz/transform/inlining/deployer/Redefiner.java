/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.deployer;

/**
 * Interface that all "redefiner" implementations should implement.
 * <p/>
 * Redefines all classes at all points defined by the <code>ChangeSet</code> passed to the
 * <code>redefine</code> method.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface Redefiner {

  /**
   * Redefines all classes affected by the change set according to the rules defined in the change set.
   *
   * @param changeSet
   */
  void redefine(ChangeSet changeSet);
}
