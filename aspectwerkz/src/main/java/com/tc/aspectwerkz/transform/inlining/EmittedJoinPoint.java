/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining;

import com.tc.asm.Label;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.transform.InstrumentationContext;

/**
 * A structure that keeps required information needed to regenerate a JIT joinpoint. The weaver emits this
 * information so that we can add initalization code to the weaved class. Note that EmittedJP are really Emitted -
 * and can be a subset of actual JP (f.e. call, where information is lost in between each weave phase).
 * <p/>
 * FIXME equals and hashcode are wrong if 2 JP in same withincode - should depend on line number f.e. but that won't
 * even be enough. Muts have a static variable and trust that creation of EmittedJP is ok.
 * Check where those are used in a map for hashcode / equals to be used.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class EmittedJoinPoint {

  public final static Label NO_LINE_NUMBER = new Label();

  private final int joinPointType;
  private final String callerClassName;
  private final String callerMethodName;
  private final String callerMethodDesc;
  private final int callerMethodModifiers;
  private final String calleeClassName;
  private final String calleeMemberName;
  private final String calleeMemberDesc;
  private final int calleeMemberModifiers;
  private final int joinPointHash;
  private final String joinPointClassName;
  private final Label lineNumberLabel;

  /**
   * Line number for call / getDefault / set / handler joinpoint
   * The lineNumber is 0 unless available and resolveLineNumber(Context) has been called.
   */
  private int lineNumber = 0;

  /**
   * Creates a new instance.
   *
   * @param joinPointType
   * @param callerClassName
   * @param callerMethodName
   * @param callerMethodDesc
   * @param callerMethodModifiers
   * @param calleeClassName
   * @param calleeMemberName
   * @param calleeMemberDesc
   * @param calleeMemberModifiers
   * @param joinPointHash
   * @param joinPointClassName
   * @param lineNumberLabel
   */
  public EmittedJoinPoint(final int joinPointType,
                          final String callerClassName,
                          final String callerMethodName,
                          final String callerMethodDesc,
                          final int callerMethodModifiers,
                          final String calleeClassName,
                          final String calleeMemberName,
                          final String calleeMemberDesc,
                          final int calleeMemberModifiers,
                          final int joinPointHash,
                          final String joinPointClassName,
                          final Label lineNumberLabel) {
    this.joinPointType = joinPointType;
    this.callerClassName = callerClassName;
    this.callerMethodName = callerMethodName;
    this.callerMethodDesc = callerMethodDesc;
    this.callerMethodModifiers = callerMethodModifiers;
    this.calleeClassName = calleeClassName;
    this.calleeMemberName = calleeMemberName;
    this.calleeMemberDesc = calleeMemberDesc;
    this.calleeMemberModifiers = calleeMemberModifiers;
    this.joinPointHash = joinPointHash;
    this.joinPointClassName = joinPointClassName;
    this.lineNumberLabel = lineNumberLabel;
  }

  /**
   * Creates a new instance.
   *
   * @param joinPointType
   * @param callerClassName
   * @param callerMethodName
   * @param callerMethodDesc
   * @param callerMethodModifiers
   * @param calleeClassName
   * @param calleeMemberName
   * @param calleeMemberDesc
   * @param calleeMemberModifiers
   * @param joinPointHash
   * @param joinPointClassName
   */
  public EmittedJoinPoint(final int joinPointType,
                          final String callerClassName,
                          final String callerMethodName,
                          final String callerMethodDesc,
                          final int callerMethodModifiers,
                          final String calleeClassName,
                          final String calleeMemberName,
                          final String calleeMemberDesc,
                          final int calleeMemberModifiers,
                          final int joinPointHash,
                          final String joinPointClassName) {
    this(joinPointType, callerClassName, callerMethodName, callerMethodDesc, callerMethodModifiers,
            calleeClassName, calleeMemberName, calleeMemberDesc, calleeMemberModifiers,
            joinPointHash, joinPointClassName, NO_LINE_NUMBER
    );
  }

  public int getJoinPointType() {
    return joinPointType;
  }

  public String getCallerClassName() {
    return callerClassName;
  }

  public String getCallerMethodName() {
    return callerMethodName;
  }

  public String getCallerMethodDesc() {
    return callerMethodDesc;
  }

  public int getCallerMethodModifiers() {
    return callerMethodModifiers;
  }

  public String getCalleeClassName() {
    return calleeClassName;
  }

  public String getCalleeMemberName() {
    return calleeMemberName;
  }

  public String getCalleeMemberDesc() {
    return calleeMemberDesc;
  }

  public int getCalleeMemberModifiers() {
    return calleeMemberModifiers;
  }

  public int getJoinPointHash() {
    return joinPointHash;
  }

  public String getJoinPointClassName() {
    return joinPointClassName;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void resolveLineNumber(InstrumentationContext context) {
    lineNumber = context.resolveLineNumberInfo(lineNumberLabel);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EmittedJoinPoint)) {
      return false;
    }

    final EmittedJoinPoint emittedJoinPoint = (EmittedJoinPoint) o;

    if (calleeMemberModifiers != emittedJoinPoint.calleeMemberModifiers) {
      return false;
    }
    if (callerMethodModifiers != emittedJoinPoint.callerMethodModifiers) {
      return false;
    }
    if (joinPointHash != emittedJoinPoint.joinPointHash) {
      return false;
    }
    if (joinPointType != emittedJoinPoint.joinPointType) {
      return false;
    }
    if (!calleeClassName.equals(emittedJoinPoint.calleeClassName)) {
      return false;
    }
    if (!calleeMemberDesc.equals(emittedJoinPoint.calleeMemberDesc)) {
      return false;
    }
    if (!calleeMemberName.equals(emittedJoinPoint.calleeMemberName)) {
      return false;
    }
    if (!callerClassName.equals(emittedJoinPoint.callerClassName)) {
      return false;
    }
    if (!callerMethodDesc.equals(emittedJoinPoint.callerMethodDesc)) {
      return false;
    }
    if (!callerMethodName.equals(emittedJoinPoint.callerMethodName)) {
      return false;
    }
    if (!joinPointClassName.equals(emittedJoinPoint.joinPointClassName)) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result;
    result = joinPointType;
    result = 29 * result + callerClassName.hashCode();
    result = 29 * result + callerMethodName.hashCode();
    result = 29 * result + callerMethodDesc.hashCode();
    result = 29 * result + callerMethodModifiers;
    result = 29 * result + calleeClassName.hashCode();
    result = 29 * result + calleeMemberName.hashCode();
    result = 29 * result + calleeMemberDesc.hashCode();
    result = 29 * result + calleeMemberModifiers;
    result = 29 * result + joinPointHash;
    result = 29 * result + joinPointClassName.hashCode();
    return result;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(JoinPointType.fromInt(getJoinPointType()).toString());
    sb.append(" , caller ");
    sb.append(getCallerClassName());
    sb.append('.').append(getCallerMethodName());
    sb.append(getCallerMethodDesc());
    sb.append(" , callee ");
    sb.append(getCalleeClassName());
    sb.append('.').append(getCalleeMemberName());
    sb.append(' ').append(getCalleeMemberDesc());
    sb.append(" , line ").append(getLineNumber());
    return sb.toString();
  }
}