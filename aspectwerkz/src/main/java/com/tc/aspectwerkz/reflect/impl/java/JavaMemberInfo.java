/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;


import java.lang.reflect.Member;
import java.util.List;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public abstract class JavaMemberInfo implements MemberInfo {
  /**
   * The member.
   */
  protected final Member m_member;

  /**
   * The declaring type.
   */
  protected final ClassInfo m_declaringType;

  /**
   * The attributes.
   */
  protected List m_annotations = null;

  /**
   * The class info repository.
   */
  protected final JavaClassInfoRepository m_classInfoRepository;

  /**
   * Creates a new member meta data instance.
   *
   * @param member
   * @param declaringType
   */
  JavaMemberInfo(final Member member, final JavaClassInfo declaringType) {
    if (member == null) {
      throw new IllegalArgumentException("member can not be null");
    }
    if (declaringType == null) {
      throw new IllegalArgumentException("declaring type can not be null");
    }
    m_member = member;
    m_declaringType = declaringType;
    m_classInfoRepository = JavaClassInfoRepository.getRepository(member.getDeclaringClass().getClassLoader());
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return m_member.getName();
  }

  /**
   * Returns the modifiers.
   *
   * @return the modifiers
   */
  public int getModifiers() {
    return m_member.getModifiers();
  }

  /**
   * Returns the declaring type.
   *
   * @return the declaring type
   */
  public ClassInfo getDeclaringType() {
    return m_declaringType;
  }
}