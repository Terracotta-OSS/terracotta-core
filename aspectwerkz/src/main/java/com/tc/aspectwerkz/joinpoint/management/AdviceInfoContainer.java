/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.management;

import java.util.List;
import java.util.ArrayList;

import com.tc.aspectwerkz.aspect.AdviceInfo;

/**
 * Container for the advice infos that belongs to a specific join point.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AdviceInfoContainer {

  /**
   * Null AdviceInfoContainer instance
   */
  public static final AdviceInfoContainer NULL;

  static {
    NULL = new AdviceInfoContainer(
            new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()
    );
  }

  private final AdviceInfo[] m_aroundAdvices;
  private final AdviceInfo[] m_beforeAdvices;
  private final AdviceInfo[] m_afterFinallyAdvices;
  private final AdviceInfo[] m_afterReturningAdvices;
  private final AdviceInfo[] m_afterThrowingAdvices;

  /**
   * Creates a advice info container.
   *
   * @param aroundAdvices
   * @param beforeAdvices
   * @param afterFinallyAdvices
   * @param afterReturningAdvices
   * @param afterThrowingAdvices
   */
  public AdviceInfoContainer(final List aroundAdvices,
                             final List beforeAdvices,
                             final List afterFinallyAdvices,
                             final List afterReturningAdvices,
                             final List afterThrowingAdvices) {
    m_aroundAdvices = (AdviceInfo[]) aroundAdvices.toArray(AdviceInfo.EMPTY_ADVICE_INFO_ARRAY);
    m_beforeAdvices = (AdviceInfo[]) beforeAdvices.toArray(AdviceInfo.EMPTY_ADVICE_INFO_ARRAY);
    m_afterFinallyAdvices = (AdviceInfo[]) afterFinallyAdvices.toArray(AdviceInfo.EMPTY_ADVICE_INFO_ARRAY);
    m_afterReturningAdvices = (AdviceInfo[]) afterReturningAdvices.toArray(AdviceInfo.EMPTY_ADVICE_INFO_ARRAY);
    m_afterThrowingAdvices = (AdviceInfo[]) afterThrowingAdvices.toArray(AdviceInfo.EMPTY_ADVICE_INFO_ARRAY);
  }

  /**
   * Returns the around advice infos.
   *
   * @return
   */
  public AdviceInfo[] getAroundAdviceInfos() {
    return m_aroundAdvices;
  }

  /**
   * Returns the before advice infos.
   *
   * @return
   */
  public AdviceInfo[] getBeforeAdviceInfos() {
    return m_beforeAdvices;
  }

  /**
   * Returns the after finally advice infos.
   *
   * @return
   */
  public AdviceInfo[] getAfterFinallyAdviceInfos() {
    return m_afterFinallyAdvices;
  }

  /**
   * Returns the after returning advice infos.
   *
   * @return
   */
  public AdviceInfo[] getAfterReturningAdviceInfos() {
    return m_afterReturningAdvices;
  }

  /**
   * Returns the after throwing advice infos.
   *
   * @return
   */
  public AdviceInfo[] getAfterThrowingAdviceInfos() {
    return m_afterThrowingAdvices;
  }

  /**
   * Return all advice infos.
   *
   * @return
   */
  public AdviceInfo[] getAllAdviceInfos() {
    int size = m_beforeAdvices.length + m_aroundAdvices.length + m_afterReturningAdvices.length
            + m_afterThrowingAdvices.length + m_afterFinallyAdvices.length;
    AdviceInfo[] advices = new AdviceInfo[size];

    int destPos = 0;
    System.arraycopy(m_beforeAdvices, 0, advices, destPos, m_beforeAdvices.length);
    destPos += m_beforeAdvices.length;
    System.arraycopy(m_aroundAdvices, 0, advices, destPos, m_aroundAdvices.length);
    destPos += m_aroundAdvices.length;
    System.arraycopy(m_afterReturningAdvices, 0, advices, destPos, m_afterReturningAdvices.length);
    destPos += m_afterReturningAdvices.length;
    System.arraycopy(m_afterThrowingAdvices, 0, advices, destPos, m_afterThrowingAdvices.length);
    destPos += m_afterThrowingAdvices.length;
    System.arraycopy(m_afterFinallyAdvices, 0, advices, destPos, m_afterFinallyAdvices.length);
    destPos += m_afterFinallyAdvices.length;

    return advices;
  }

}
