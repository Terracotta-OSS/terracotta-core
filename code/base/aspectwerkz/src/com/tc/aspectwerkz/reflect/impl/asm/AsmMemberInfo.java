/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.asm;

import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.lang.ref.WeakReference;

/**
 * ASM implementation of the MemberInfo interface.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public abstract class AsmMemberInfo implements MemberInfo {

  /**
   * The member info.
   */
  protected final MemberStruct m_member;

  /**
   * The class loader wrapped in a weak ref.
   */
  protected final WeakReference m_loaderRef;

  /**
   * The declaring type name.
   */
  protected final String m_declaringTypeName;

  /**
   * The declaring type.
   */
  protected ClassInfo m_declaringType;

  /**
   * The class info repository.
   */
  // protected final AsmClassInfoRepository m_classInfoRepository;

  /**
   * Creates a new member meta data instance.
   *
   * @param member
   * @param declaringType
   * @param loader
   */
  AsmMemberInfo(final MemberStruct member, final String declaringType, final ClassLoader loader) {
    if (member == null) {
      throw new IllegalArgumentException("member can not be null");
    }
    if (declaringType == null) {
      throw new IllegalArgumentException("declaring type can not be null");
    }
    m_member = member;
    m_loaderRef = new WeakReference(loader);
    m_declaringTypeName = declaringType.replace('/', '.');
    // m_classInfoRepository = AsmClassInfoRepository.getRepository(loader);
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return m_member.name;
  }

  /**
   * Returns the modifiers.
   *
   * @return the modifiers
   */
  public int getModifiers() {
    return m_member.modifiers;
  }

  /**
   * Returns the declaring type.
   *
   * @return the declaring type
   */
  public ClassInfo getDeclaringType() {
    if (m_declaringType == null) {
      // m_declaringType = m_classInfoRepository.getClassInfo(m_declaringTypeName);
      m_declaringType = AsmClassInfo.getClassInfo(m_declaringTypeName, (ClassLoader) m_loaderRef.get());
    }
    return m_declaringType;
  }
}