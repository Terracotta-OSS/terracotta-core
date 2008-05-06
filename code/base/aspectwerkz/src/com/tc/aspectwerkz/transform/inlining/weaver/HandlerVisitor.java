/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.Label;
import com.tc.asm.Type;

import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.TransformationUtil;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.PointcutType;

import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Modifier;

/**
 * Advises catch clauses by inserting a call to the join point as the first thing in the catch block.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 * 
 * TODO in ASM 2.x and later try/catch blocks are visited before method code, so this adapter is not needed
 */
public class HandlerVisitor extends ClassAdapter implements TransformationConstants {

  /**
   * A visitor that looks for all catch clause and keep track of them
   * providing that they match
   */
  public static class LookaheadCatchLabelsClassAdapter extends ClassAdapter {

    /**
     * list of CatchLabelStruct that matches
     */
    List m_catchLabels = new ArrayList();

    /**
     * map of Integer(index in whole class)-->asm.Label for all the visited labels
     */
    private final Map m_labelIndexes = new HashMap();

    /**
     * current label index in whole class, from 0 to N
     */
    private int m_labelIndex = -1;

    private final InstrumentationContext m_ctx;
    private final ClassLoader m_loader;
    private final ClassInfo m_callerClassInfo;

    /**
     * Visit the class
     *
     * @param cv
     * @param loader
     * @param callerClassInfo
     * @param ctx
     * @param catchLabels
     */
    public LookaheadCatchLabelsClassAdapter(ClassVisitor cv, ClassLoader loader, ClassInfo callerClassInfo,
                                            InstrumentationContext ctx, List catchLabels) {
      super(cv);
      m_catchLabels = catchLabels;
      m_loader = loader;
      m_callerClassInfo = callerClassInfo;
      m_ctx = ctx;
    }

    /**
     * Visit method bodies
     *
     * @param access
     * @param callerMethodName
     * @param callerMethodDesc
     * @param exceptions
     * @return
     */
    public MethodVisitor visitMethod(final int access,
                                     final String callerMethodName,
                                     final String callerMethodDesc,
                                     final String callerMethodsignature,
                                     final String[] exceptions) {
      if (callerMethodName.startsWith(WRAPPER_METHOD_PREFIX)) {
        return super.visitMethod(access, callerMethodName, callerMethodDesc, callerMethodsignature, exceptions);
      }

      MethodVisitor mv = cv.visitMethod(access, callerMethodName, callerMethodDesc, callerMethodsignature, exceptions);
      if (mv == null) {
        return mv;
      }

      final MemberInfo callerMemberInfo;
      if (CLINIT_METHOD_NAME.equals(callerMethodName)) {
        callerMemberInfo = m_callerClassInfo.staticInitializer();
      } else if (INIT_METHOD_NAME.equals(callerMethodName)) {
        int hash = AsmHelper.calculateConstructorHash(callerMethodDesc);
        callerMemberInfo = m_callerClassInfo.getConstructor(hash);
      } else {
        int hash = AsmHelper.calculateMethodHash(callerMethodName, callerMethodDesc);
        callerMemberInfo = m_callerClassInfo.getMethod(hash);
      }
      if (callerMemberInfo == null) {
        System.err.println(
                "AW::WARNING " +
                        "metadata structure could not be build for method ["
                        + m_callerClassInfo.getName().replace('/', '.')
                        + '.' + callerMethodName + ':' + callerMethodDesc + ']'
        );
        return mv;
      }

      return new LookaheadCatchLabelsMethodAdapter(mv, callerMemberInfo, this);
    }
  }

  /**
   * Visit the method, and keep track of all labels so that when visittryCatch is reached
   * we can remember the index
   */
  static final class LookaheadCatchLabelsMethodAdapter extends MethodAdapter {
    private final MemberInfo info;
    private final LookaheadCatchLabelsClassAdapter classAdapter;

    LookaheadCatchLabelsMethodAdapter(MethodVisitor mv, MemberInfo info, LookaheadCatchLabelsClassAdapter classAdapter) {
      super(mv);
      this.info = info;
      this.classAdapter = classAdapter;
    }

    public void visitLabel(Label label) {
      classAdapter.m_labelIndexes.put(label, new Integer(++classAdapter.m_labelIndex));
      super.visitLabel(label);
    }

    public void visitTryCatchBlock(Label startLabel, Label endLabel, Label handlerLabel,
                                   String exceptionTypeName) {
      if (exceptionTypeName == null) {
        // finally block
        super.visitTryCatchBlock(startLabel, endLabel, handlerLabel, exceptionTypeName);
        return;
      }
      final ClassInfo exceptionClassInfo = AsmClassInfo.getClassInfo(exceptionTypeName, classAdapter.m_loader);
      final ExpressionContext ctx = new ExpressionContext(
              PointcutType.HANDLER, exceptionClassInfo, info
      );
      if (!handlerFilter(classAdapter.m_ctx.getDefinitions(), ctx)) {
        // remember its index and the exception exceptionClassInfo
        Integer index = (Integer) classAdapter.m_labelIndexes.get(handlerLabel);
        if (index != null) {
          classAdapter.m_catchLabels.add(
                  new CatchLabelStruct(
                          index.intValue(),
                          exceptionClassInfo,
                          classAdapter.m_callerClassInfo,
                          info
                  )
          );
        }
      }
      super.visitTryCatchBlock(startLabel, endLabel, handlerLabel, exceptionTypeName);
    }
  }
  
  
  //---- non lookahead visitor

  private final InstrumentationContext m_ctx;

  /**
   * List of matching catch clause
   */
  private final List m_catchLabels;

  /**
   * catch clause index in whole class
   */
  private int m_labelIndex = -1;

  private Label m_lastLabelForLineNumber = EmittedJoinPoint.NO_LINE_NUMBER;

  /**
   * Creates a new instance.
   *
   * @param cv
   * @param ctx
   */
  public HandlerVisitor(final ClassVisitor cv,
                        final InstrumentationContext ctx,
                        final List catchLabels) {
    super(cv);
    m_ctx = ctx;
    m_catchLabels = catchLabels;
  }

  /**
   * Visits the methods bodies to weave in JP calls at catch clauses
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @return
   */
  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    if (name.startsWith(WRAPPER_METHOD_PREFIX)) {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    return mv == null ? null : new CatchClauseCodeAdapter(mv);
  }

  /**
   * Advises catch clauses by inserting a call to the join point as the first thing in the catch block.
   *
   * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
   */
  public class CatchClauseCodeAdapter extends MethodAdapter {

    /**
     * Creates a new instance.
     *
     * @param ca
     */
    public CatchClauseCodeAdapter(final MethodVisitor ca) {
      super(ca);
    }

    public void visitLabel(Label label) {
      m_lastLabelForLineNumber = label;
      super.visitLabel(label);

      // check if it is a catch label
      int index = ++m_labelIndex;
      CatchLabelStruct catchLabel = null;
      for (Iterator iterator = m_catchLabels.iterator(); iterator.hasNext();) {
        CatchLabelStruct aCatchLabel = (CatchLabelStruct) iterator.next();
        if (aCatchLabel.labelIndexInWholeClass == index) {
          catchLabel = aCatchLabel;
          break;
        }
      }
      if (catchLabel == null) {
        return;
      }
      // matched
      m_ctx.markAsAdvised();
      final String callerTypeName = catchLabel.caller.getName().replace('.', '/');
      final String exceptionTypeDesc = catchLabel.exception.getSignature();
      final String exceptionTypeName = Type.getType(exceptionTypeDesc).getInternalName();
      final int joinPointHash = AsmHelper.calculateClassHash(exceptionTypeDesc);
      final String joinPointClassName = TransformationUtil.getJoinPointClassName(
              callerTypeName,
              catchLabel.callerMember.getName(),
              catchLabel.callerMember.getSignature(),
              exceptionTypeName,
              JoinPointType.HANDLER_INT,
              joinPointHash
      );

      // add the call to the join point
      // exception instance is on the stack
      // dup it for ARG0
      mv.visitInsn(DUP);

      // load caller instance if any
      if (Modifier.isStatic(catchLabel.callerMember.getModifiers())) {
        mv.visitInsn(ACONST_NULL);
      } else {
        mv.visitVarInsn(ALOAD, 0);
      }
      //TODO for now we pass the exception as both CALLEE and ARG0 - may be callee must be NULL
      //? check in AJ RTTI
      mv.visitMethodInsn(
              INVOKESTATIC, joinPointClassName, INVOKE_METHOD_NAME,
              TransformationUtil.getInvokeSignatureForHandlerJoinPoints(callerTypeName, exceptionTypeName)
      );

      // emit the joinpoint
      m_ctx.addEmittedJoinPoint(
              new EmittedJoinPoint(
                      JoinPointType.HANDLER_INT,
                      callerTypeName,
                      catchLabel.callerMember.getName(),
                      catchLabel.callerMember.getSignature(),
                      catchLabel.callerMember.getModifiers(),
                      exceptionTypeName,
                      "",
                      exceptionTypeDesc,
                      0, // a bit meaningless but must not be static
                      joinPointHash,
                      joinPointClassName,
                      m_lastLabelForLineNumber
              )
      );
    }
  }

  /**
   * Filters out the catch clauses that are not eligible for transformation.
   *
   * @param definitions
   * @param ctx
   * @return boolean true if the catch clause should be filtered out
   */
  static boolean handlerFilter(final Set definitions, final ExpressionContext ctx) {
    for (Iterator it = definitions.iterator(); it.hasNext();) {
      if (((SystemDefinition) it.next()).hasPointcut(ctx)) {
        return false;
      } else {
        continue;
      }
    }
    return true;
  }

  /**
   * A struct to represent a catch clause.
   * The index is class wide, and the exception class info is kept.
   */
  static class CatchLabelStruct {
    int labelIndexInWholeClass = -1;
    ClassInfo exception = null;
    ClassInfo caller = null;
    MemberInfo callerMember = null;

    CatchLabelStruct(int indexInClass, ClassInfo exception, ClassInfo caller, MemberInfo callerMember) {
      labelIndexInWholeClass = indexInClass;
      this.exception = exception;
      this.caller = caller;
      this.callerMember = callerMember;
    }
  }
}