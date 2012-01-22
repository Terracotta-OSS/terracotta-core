/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.deployer;

/**
 * Interface that all "redefiner" implementations should implement.
 * <p/>
 * Redefines all classes at all points defined by the <code>ChangeSet</code> passed to the
 * <code>redefine</code> method.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface Redefiner {

  /**
   * Redefines all classes affected by the change set according to the rules defined in the change set.
   *
   * @param changeSet
   */
  void redefine(ChangeSet changeSet);
}
