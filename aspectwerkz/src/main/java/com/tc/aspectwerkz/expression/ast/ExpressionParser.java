/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression.ast;

import com.tc.util.FindbugsSuppressWarnings;

import java.lang.reflect.Modifier;
import java.io.Reader;
import java.io.StringReader;

/**
 * Usage:
 * <pre>
 *     ExpressionParser parser = new ExpressionParser(System.in); // can be only one
 *     ASTRoot root = parser.parse("call(@RequiresNew public * foo.Bar.*(String, ..) AND withincode(* foo.Baz.within(..)");
 *     Expression expression = new Expression(root);
 *     ...
 *  </pre>
 * <p/>
 * <p/>
 * TODO: the grammar is still fragile
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr</a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class ExpressionParser/*@bgen(jjtree)*/ implements ExpressionParserTreeConstants, ExpressionParserConstants {/*@bgen(jjtree)*/
  protected static final JJTExpressionParserState jjtree = new JJTExpressionParserState();

  public ASTRoot parse(String expression) throws ParseException {
    return parse(new StringReader(expression));
  }

  public ASTRoot parse(Reader reader) throws ParseException {
    ReInit(reader);
    return Root();
  }

//------------------ Bootstrap ------------------

  /**
   * Entry point.
   */
  static final public ASTRoot Root() throws ParseException {
    /*@bgen(jjtree) Root */
    ASTRoot jjtn000 = new ASTRoot(JJTROOT);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      Expression();
      jj_consume_token(0);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      {
        if (true) return jjtn000;
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
    throw new Error("Missing return statement in function");
  }

  /**
   * Expression.
   */
  static final public void Expression() throws ParseException {
    /*@bgen(jjtree) Expression */
    ASTExpression jjtn000 = new ASTExpression(JJTEXPRESSION);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      AndExpression();
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

//------------------ Logical operators ------------------

  /**
   * AndExpression.
   */
  static final public void AndExpression() throws ParseException {
    ASTAnd jjtn001 = new ASTAnd(JJTAND);
    boolean jjtc001 = true;
    jjtree.openNodeScope(jjtn001);
    try {
      OrExpression();
      label_1:
      while (true) {
        if (jj_2_1(2)) {
          ;
        } else {
          break label_1;
        }
        jj_consume_token(AND);
        OrExpression();
      }
    } catch (Throwable jjte001) {
      if (jjtc001) {
        jjtree.clearNodeScope(jjtn001);
        jjtc001 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte001 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte001;
        }
      }
      if (jjte001 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte001;
        }
      }
      {
        if (true) throw (Error) jjte001;
      }
    } finally {
      if (jjtc001) {
        jjtree.closeNodeScope(jjtn001, jjtree.nodeArity() > 1);
      }
    }
  }

  /**
   * OrExpression.
   */
  static final public void OrExpression() throws ParseException {
    ASTOr jjtn001 = new ASTOr(JJTOR);
    boolean jjtc001 = true;
    jjtree.openNodeScope(jjtn001);
    try {
      UnaryExpression();
      label_2:
      while (true) {
        if (jj_2_2(2)) {
          ;
        } else {
          break label_2;
        }
        jj_consume_token(OR);
        AndExpression();
      }
    } catch (Throwable jjte001) {
      if (jjtc001) {
        jjtree.clearNodeScope(jjtn001);
        jjtc001 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte001 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte001;
        }
      }
      if (jjte001 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte001;
        }
      }
      {
        if (true) throw (Error) jjte001;
      }
    } finally {
      if (jjtc001) {
        jjtree.closeNodeScope(jjtn001, jjtree.nodeArity() > 1);
      }
    }
  }

  /**
   * UnaryExpression.
   */
  static final public void UnaryExpression() throws ParseException {
    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
      case NOT:
        NotExpression();
        break;
      case 86:
        jj_consume_token(86);
        Expression();
        jj_consume_token(87);
        break;
      case EXECUTION:
      case CALL:
      case SET:
      case GET:
      case HANDLER:
      case WITHIN:
      case WITHIN_CODE:
      case STATIC_INITIALIZATION:
      case CFLOW:
      case CFLOW_BELOW:
      case ARGS:
      case TARGET:
      case THIS:
      case IF:
      case HAS_METHOD:
      case HAS_FIELD:
      case POINTCUT_REFERENCE_WITH_ARGS:
      case POINTCUT_REFERENCE:
        Pointcut();
        break;
      default:
        jj_la1[0] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
    }
  }

  /**
   * NotExpression.
   */
  static final public void NotExpression() throws ParseException {
    jj_consume_token(NOT);
    ASTNot jjtn001 = new ASTNot(JJTNOT);
    boolean jjtc001 = true;
    jjtree.openNodeScope(jjtn001);
    try {
      UnaryExpression();
    } catch (Throwable jjte001) {
      if (jjtc001) {
        jjtree.clearNodeScope(jjtn001);
        jjtc001 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte001 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte001;
        }
      }
      if (jjte001 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte001;
        }
      }
      {
        if (true) throw (Error) jjte001;
      }
    } finally {
      if (jjtc001) {
        jjtree.closeNodeScope(jjtn001, true);
      }
    }
  }

//------------------ Pointcuts ------------------

  /**
   * Pointcut.
   */
  static final public void Pointcut() throws ParseException {
    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
      case CALL:
        Call();
        break;
      case EXECUTION:
        Execution();
        break;
      case WITHIN_CODE:
        WithinCode();
        break;
      case HAS_METHOD:
        HasMethod();
        break;
      case SET:
        Set();
        break;
      case GET:
        Get();
        break;
      case HAS_FIELD:
        HasField();
        break;
      case WITHIN:
        Within();
        break;
      case HANDLER:
        Handler();
        break;
      case ARGS:
        Args();
        break;
      case TARGET:
        Target();
        break;
      case THIS:
        This();
        break;
      case CFLOW:
        Cflow();
        break;
      case CFLOW_BELOW:
        CflowBelow();
        break;
      case STATIC_INITIALIZATION:
        StaticInitialization();
        break;
      case IF:
        If();
        break;
      case POINTCUT_REFERENCE_WITH_ARGS:
      case POINTCUT_REFERENCE:
        PointcutReference();
        break;
      default:
        jj_la1[1] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
    }
  }

  /**
   * Pointcut reference.
   */
  static final public void PointcutReference() throws ParseException {
    /*@bgen(jjtree) PointcutReference */
    ASTPointcutReference jjtn000 = new ASTPointcutReference(JJTPOINTCUTREFERENCE);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token name;
    try {
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case POINTCUT_REFERENCE_WITH_ARGS:
          name = jj_consume_token(POINTCUT_REFERENCE_WITH_ARGS);
          break;
        case POINTCUT_REFERENCE:
          name = jj_consume_token(POINTCUT_REFERENCE);
          break;
        default:
          jj_la1[2] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtn000.setName(name.image);
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case COMMA:
        case EAGER_WILDCARD:
        case ARG_PATTERN:
        case ARG_ARRAY_PATTERN:
        case ARGS_END:
          ArgsParameters();
          jj_consume_token(ARGS_END);
          break;
        default:
          jj_la1[3] = jj_gen;
          ;
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Execution.
   */
  static final public void Execution() throws ParseException {
    /*@bgen(jjtree) Execution */
    ASTExecution jjtn000 = new ASTExecution(JJTEXECUTION);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(EXECUTION);
      label_3:
      while (true) {
        if (jj_2_3(2)) {
          ;
        } else {
          break label_3;
        }
        MethodAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PARAMETER_END:
          jj_consume_token(METHOD_PARAMETER_END);
          break;
        case METHOD_PUBLIC:
        case METHOD_PROTECTED:
        case METHOD_PRIVATE:
        case METHOD_STATIC:
        case METHOD_ABSTRACT:
        case METHOD_FINAL:
        case METHOD_NATIVE:
        case METHOD_SYNCHRONIZED:
        case METHOD_NOT:
        case METHOD_CLASS_PATTERN:
        case METHOD_ARRAY_CLASS_PATTERN:
        case 87:
          switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case METHOD_PUBLIC:
            case METHOD_PROTECTED:
            case METHOD_PRIVATE:
            case METHOD_STATIC:
            case METHOD_ABSTRACT:
            case METHOD_FINAL:
            case METHOD_NATIVE:
            case METHOD_SYNCHRONIZED:
            case METHOD_NOT:
            case METHOD_CLASS_PATTERN:
            case METHOD_ARRAY_CLASS_PATTERN:
              if (jj_2_4(4)) {
                ConstructorPattern();
              } else {
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                  case METHOD_PUBLIC:
                  case METHOD_PROTECTED:
                  case METHOD_PRIVATE:
                  case METHOD_STATIC:
                  case METHOD_ABSTRACT:
                  case METHOD_FINAL:
                  case METHOD_NATIVE:
                  case METHOD_SYNCHRONIZED:
                  case METHOD_NOT:
                  case METHOD_CLASS_PATTERN:
                  case METHOD_ARRAY_CLASS_PATTERN:
                    MethodPattern();
                    break;
                  default:
                    jj_la1[4] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
              }
              break;
            default:
              jj_la1[5] = jj_gen;
              ;
          }
          jj_consume_token(87);
          break;
        default:
          jj_la1[6] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Call.
   */
  static final public void Call() throws ParseException {
    /*@bgen(jjtree) Call */
    ASTCall jjtn000 = new ASTCall(JJTCALL);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(CALL);
      label_4:
      while (true) {
        if (jj_2_5(2)) {
          ;
        } else {
          break label_4;
        }
        MethodAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PARAMETER_END:
          jj_consume_token(METHOD_PARAMETER_END);
          break;
        case METHOD_PUBLIC:
        case METHOD_PROTECTED:
        case METHOD_PRIVATE:
        case METHOD_STATIC:
        case METHOD_ABSTRACT:
        case METHOD_FINAL:
        case METHOD_NATIVE:
        case METHOD_SYNCHRONIZED:
        case METHOD_NOT:
        case METHOD_CLASS_PATTERN:
        case METHOD_ARRAY_CLASS_PATTERN:
        case 87:
          switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case METHOD_PUBLIC:
            case METHOD_PROTECTED:
            case METHOD_PRIVATE:
            case METHOD_STATIC:
            case METHOD_ABSTRACT:
            case METHOD_FINAL:
            case METHOD_NATIVE:
            case METHOD_SYNCHRONIZED:
            case METHOD_NOT:
            case METHOD_CLASS_PATTERN:
            case METHOD_ARRAY_CLASS_PATTERN:
              if (jj_2_6(4)) {
                ConstructorPattern();
              } else {
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                  case METHOD_PUBLIC:
                  case METHOD_PROTECTED:
                  case METHOD_PRIVATE:
                  case METHOD_STATIC:
                  case METHOD_ABSTRACT:
                  case METHOD_FINAL:
                  case METHOD_NATIVE:
                  case METHOD_SYNCHRONIZED:
                  case METHOD_NOT:
                  case METHOD_CLASS_PATTERN:
                  case METHOD_ARRAY_CLASS_PATTERN:
                    MethodPattern();
                    break;
                  default:
                    jj_la1[7] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
              }
              break;
            default:
              jj_la1[8] = jj_gen;
              ;
          }
          jj_consume_token(87);
          break;
        default:
          jj_la1[9] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Set.
   */
  static final public void Set() throws ParseException {
    /*@bgen(jjtree) Set */
    ASTSet jjtn000 = new ASTSet(JJTSET);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(SET);
      label_5:
      while (true) {
        if (jj_2_7(2)) {
          ;
        } else {
          break label_5;
        }
        FieldAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case FIELD_PRIVATE:
        case FIELD_PROTECTED:
        case FIELD_PUBLIC:
        case FIELD_STATIC:
        case FIELD_ABSTRACT:
        case FIELD_FINAL:
        case FIELD_TRANSIENT:
        case FIELD_NOT:
        case FIELD_CLASS_PATTERN:
        case FIELD_ARRAY_CLASS_PATTERN:
          FieldPattern();
          break;
        default:
          jj_la1[10] = jj_gen;
          ;
      }
      jj_consume_token(FIELD_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Get.
   */
  static final public void Get() throws ParseException {
    /*@bgen(jjtree) Get */
    ASTGet jjtn000 = new ASTGet(JJTGET);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(GET);
      label_6:
      while (true) {
        if (jj_2_8(2)) {
          ;
        } else {
          break label_6;
        }
        FieldAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case FIELD_PRIVATE:
        case FIELD_PROTECTED:
        case FIELD_PUBLIC:
        case FIELD_STATIC:
        case FIELD_ABSTRACT:
        case FIELD_FINAL:
        case FIELD_TRANSIENT:
        case FIELD_NOT:
        case FIELD_CLASS_PATTERN:
        case FIELD_ARRAY_CLASS_PATTERN:
          FieldPattern();
          break;
        default:
          jj_la1[11] = jj_gen;
          ;
      }
      jj_consume_token(FIELD_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Handler.
   */
  static final public void Handler() throws ParseException {
    /*@bgen(jjtree) Handler */
    ASTHandler jjtn000 = new ASTHandler(JJTHANDLER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(HANDLER);
      ClassPattern();
      jj_consume_token(CLASS_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Within.
   */
  static final public void Within() throws ParseException {
    /*@bgen(jjtree) Within */
    ASTWithin jjtn000 = new ASTWithin(JJTWITHIN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(WITHIN);
      label_7:
      while (true) {
        if (jj_2_9(2)) {
          ;
        } else {
          break label_7;
        }
        ClassAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case EAGER_WILDCARD:
        case CLASS_PRIVATE:
        case CLASS_PROTECTED:
        case CLASS_PUBLIC:
        case CLASS_STATIC:
        case CLASS_ABSTRACT:
        case CLASS_FINAL:
        case CLASS_NOT:
        case CLASS_PATTERN:
          ClassPattern();
          break;
        default:
          jj_la1[12] = jj_gen;
          ;
      }
      jj_consume_token(CLASS_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * WithinCode.
   */
  static final public void WithinCode() throws ParseException {
    /*@bgen(jjtree) WithinCode */
    ASTWithinCode jjtn000 = new ASTWithinCode(JJTWITHINCODE);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token tkn = null;
    try {
      jj_consume_token(WITHIN_CODE);
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case TYPE_STATICINITIALIZATION:
          WithinStaticInitialization();
          jj_consume_token(87);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setStaticInitializer(true);
          break;
        case METHOD_PUBLIC:
        case METHOD_PROTECTED:
        case METHOD_PRIVATE:
        case METHOD_STATIC:
        case METHOD_ABSTRACT:
        case METHOD_FINAL:
        case METHOD_NATIVE:
        case METHOD_SYNCHRONIZED:
        case METHOD_NOT:
        case METHOD_ANNOTATION:
        case METHOD_CLASS_PATTERN:
        case METHOD_ARRAY_CLASS_PATTERN:
        case METHOD_PARAMETER_END:
        case 87:
          label_8:
          while (true) {
            if (jj_2_10(2)) {
              ;
            } else {
              break label_8;
            }
            MethodAttribute();
          }
          switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case METHOD_PARAMETER_END:
              jj_consume_token(METHOD_PARAMETER_END);
              break;
            case METHOD_PUBLIC:
            case METHOD_PROTECTED:
            case METHOD_PRIVATE:
            case METHOD_STATIC:
            case METHOD_ABSTRACT:
            case METHOD_FINAL:
            case METHOD_NATIVE:
            case METHOD_SYNCHRONIZED:
            case METHOD_NOT:
            case METHOD_CLASS_PATTERN:
            case METHOD_ARRAY_CLASS_PATTERN:
            case 87:
              switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case METHOD_PUBLIC:
                case METHOD_PROTECTED:
                case METHOD_PRIVATE:
                case METHOD_STATIC:
                case METHOD_ABSTRACT:
                case METHOD_FINAL:
                case METHOD_NATIVE:
                case METHOD_SYNCHRONIZED:
                case METHOD_NOT:
                case METHOD_CLASS_PATTERN:
                case METHOD_ARRAY_CLASS_PATTERN:
                  if (jj_2_11(4)) {
                    ConstructorPattern();
                  } else {
                    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                      case METHOD_PUBLIC:
                      case METHOD_PROTECTED:
                      case METHOD_PRIVATE:
                      case METHOD_STATIC:
                      case METHOD_ABSTRACT:
                      case METHOD_FINAL:
                      case METHOD_NATIVE:
                      case METHOD_SYNCHRONIZED:
                      case METHOD_NOT:
                      case METHOD_CLASS_PATTERN:
                      case METHOD_ARRAY_CLASS_PATTERN:
                        MethodPattern();
                        break;
                      default:
                        jj_la1[13] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                    }
                  }
                  break;
                default:
                  jj_la1[14] = jj_gen;
                  ;
              }
              jj_consume_token(87);
              break;
            default:
              jj_la1[15] = jj_gen;
              jj_consume_token(-1);
              throw new ParseException();
          }
          break;
        default:
          jj_la1[16] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  static final public void WithinStaticInitialization() throws ParseException {
    /*@bgen(jjtree) StaticInitialization */
    ASTStaticInitialization jjtn000 = new ASTStaticInitialization(JJTSTATICINITIALIZATION);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(TYPE_STATICINITIALIZATION);
      jj_consume_token(METHOD_PARAMETER_START);
      label_9:
      while (true) {
        if (jj_2_12(2)) {
          ;
        } else {
          break label_9;
        }
        MethodAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PUBLIC:
        case METHOD_PROTECTED:
        case METHOD_PRIVATE:
        case METHOD_STATIC:
        case METHOD_ABSTRACT:
        case METHOD_FINAL:
        case METHOD_NOT:
        case METHOD_CLASS_PATTERN:
          StaticInitializationPattern();
          break;
        default:
          jj_la1[17] = jj_gen;
          ;
      }
      jj_consume_token(METHOD_PARAMETER_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  static final public void StaticInitializationPattern() throws ParseException {
    /*@bgen(jjtree) ClassPattern */
    ASTClassPattern jjtn000 = new ASTClassPattern(JJTCLASSPATTERN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token tkn = null;
    try {
      label_10:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_PUBLIC:
          case METHOD_PROTECTED:
          case METHOD_PRIVATE:
          case METHOD_STATIC:
          case METHOD_ABSTRACT:
          case METHOD_FINAL:
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[18] = jj_gen;
            break label_10;
        }
        StaticInitializationPatternModifier();
      }
      tkn = jj_consume_token(METHOD_CLASS_PATTERN);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setTypePattern(tkn.image);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * StaticInitialization.
   */
  static final public void StaticInitialization() throws ParseException {
    /*@bgen(jjtree) StaticInitialization */
    ASTStaticInitialization jjtn000 = new ASTStaticInitialization(JJTSTATICINITIALIZATION);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(STATIC_INITIALIZATION);
      label_11:
      while (true) {
        if (jj_2_13(2)) {
          ;
        } else {
          break label_11;
        }
        ClassAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case EAGER_WILDCARD:
        case CLASS_PRIVATE:
        case CLASS_PROTECTED:
        case CLASS_PUBLIC:
        case CLASS_STATIC:
        case CLASS_ABSTRACT:
        case CLASS_FINAL:
        case CLASS_NOT:
        case CLASS_PATTERN:
          ClassPattern();
          break;
        default:
          jj_la1[19] = jj_gen;
          ;
      }
      jj_consume_token(CLASS_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Cflow.
   */
  static final public void Cflow() throws ParseException {
    /*@bgen(jjtree) Cflow */
    ASTCflow jjtn000 = new ASTCflow(JJTCFLOW);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(CFLOW);
      Expression();
      jj_consume_token(87);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * CflowBelow.
   */
  static final public void CflowBelow() throws ParseException {
    /*@bgen(jjtree) CflowBelow */
    ASTCflowBelow jjtn000 = new ASTCflowBelow(JJTCFLOWBELOW);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(CFLOW_BELOW);
      Expression();
      jj_consume_token(87);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Args.
   */
  static final public void Args() throws ParseException {
    /*@bgen(jjtree) Args */
    ASTArgs jjtn000 = new ASTArgs(JJTARGS);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      if (jj_2_14(2)) {
        jj_consume_token(ARGS);
        jj_consume_token(ARGS_END);
      } else {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case ARGS:
            jj_consume_token(ARGS);
            ArgsParameters();
            jj_consume_token(ARGS_END);
            break;
          default:
            jj_la1[20] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * HasMethod.
   */
  static final public void HasMethod() throws ParseException {
    /*@bgen(jjtree) HasMethod */
    ASTHasMethod jjtn000 = new ASTHasMethod(JJTHASMETHOD);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(HAS_METHOD);
      label_12:
      while (true) {
        if (jj_2_15(2)) {
          ;
        } else {
          break label_12;
        }
        MethodAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PARAMETER_END:
          jj_consume_token(METHOD_PARAMETER_END);
          break;
        case METHOD_PUBLIC:
        case METHOD_PROTECTED:
        case METHOD_PRIVATE:
        case METHOD_STATIC:
        case METHOD_ABSTRACT:
        case METHOD_FINAL:
        case METHOD_NATIVE:
        case METHOD_SYNCHRONIZED:
        case METHOD_NOT:
        case METHOD_CLASS_PATTERN:
        case METHOD_ARRAY_CLASS_PATTERN:
        case 87:
          switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case METHOD_PUBLIC:
            case METHOD_PROTECTED:
            case METHOD_PRIVATE:
            case METHOD_STATIC:
            case METHOD_ABSTRACT:
            case METHOD_FINAL:
            case METHOD_NATIVE:
            case METHOD_SYNCHRONIZED:
            case METHOD_NOT:
            case METHOD_CLASS_PATTERN:
            case METHOD_ARRAY_CLASS_PATTERN:
              if (jj_2_16(4)) {
                ConstructorPattern();
              } else {
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                  case METHOD_PUBLIC:
                  case METHOD_PROTECTED:
                  case METHOD_PRIVATE:
                  case METHOD_STATIC:
                  case METHOD_ABSTRACT:
                  case METHOD_FINAL:
                  case METHOD_NATIVE:
                  case METHOD_SYNCHRONIZED:
                  case METHOD_NOT:
                  case METHOD_CLASS_PATTERN:
                  case METHOD_ARRAY_CLASS_PATTERN:
                    MethodPattern();
                    break;
                  default:
                    jj_la1[21] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
              }
              break;
            default:
              jj_la1[22] = jj_gen;
              ;
          }
          jj_consume_token(87);
          break;
        default:
          jj_la1[23] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * HasField.
   */
  static final public void HasField() throws ParseException {
    /*@bgen(jjtree) HasField */
    ASTHasField jjtn000 = new ASTHasField(JJTHASFIELD);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(HAS_FIELD);
      label_13:
      while (true) {
        if (jj_2_17(2)) {
          ;
        } else {
          break label_13;
        }
        FieldAttribute();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case FIELD_PRIVATE:
        case FIELD_PROTECTED:
        case FIELD_PUBLIC:
        case FIELD_STATIC:
        case FIELD_ABSTRACT:
        case FIELD_FINAL:
        case FIELD_TRANSIENT:
        case FIELD_NOT:
        case FIELD_CLASS_PATTERN:
        case FIELD_ARRAY_CLASS_PATTERN:
          FieldPattern();
          break;
        default:
          jj_la1[24] = jj_gen;
          ;
      }
      jj_consume_token(FIELD_POINTCUT_END);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Target
   */
  static final public void Target() throws ParseException {
    /*@bgen(jjtree) Target */
    ASTTarget jjtn000 = new ASTTarget(JJTTARGET);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token identifier;
    try {
      jj_consume_token(TARGET);
      identifier = jj_consume_token(CLASS_PATTERN);
      jjtn000.setIdentifier(identifier.image);
      jj_consume_token(CLASS_POINTCUT_END);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * This
   */
  static final public void This() throws ParseException {
    /*@bgen(jjtree) This */
    ASTThis jjtn000 = new ASTThis(JJTTHIS);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token identifier;
    try {
      jj_consume_token(THIS);
      identifier = jj_consume_token(CLASS_PATTERN);
      jjtn000.setIdentifier(identifier.image);
      jj_consume_token(CLASS_POINTCUT_END);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * If() marker
   */
  static final public void If() throws ParseException {
    /*@bgen(jjtree) If */
    ASTIf jjtn000 = new ASTIf(JJTIF);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      jj_consume_token(IF);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

//------------------ Patterns ------------------

  /**
   * Class pattern.
   */
  static final public void ClassPattern() throws ParseException {
    /*@bgen(jjtree) ClassPattern */
    ASTClassPattern jjtn000 = new ASTClassPattern(JJTCLASSPATTERN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token pattern;
    try {
      label_14:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case CLASS_PRIVATE:
          case CLASS_PROTECTED:
          case CLASS_PUBLIC:
          case CLASS_STATIC:
          case CLASS_ABSTRACT:
          case CLASS_FINAL:
          case CLASS_NOT:
            ;
            break;
          default:
            jj_la1[25] = jj_gen;
            break label_14;
        }
        ClassModifier();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case CLASS_PATTERN:
          pattern = jj_consume_token(CLASS_PATTERN);
          break;
        case EAGER_WILDCARD:
          pattern = jj_consume_token(EAGER_WILDCARD);
          break;
        default:
          jj_la1[26] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setTypePattern(pattern.image);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Method pattern.
   *
   * @TODO: split class name and method name.
   * @TODO: handle '+'.
   * @TODO: put method name, return type and declaring class in different nodes.
   */
  static final public void MethodPattern() throws ParseException {
    /*@bgen(jjtree) MethodPattern */
    ASTMethodPattern jjtn000 = new ASTMethodPattern(JJTMETHODPATTERN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token returnType, name;
    try {
      label_15:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_PUBLIC:
          case METHOD_PROTECTED:
          case METHOD_PRIVATE:
          case METHOD_STATIC:
          case METHOD_ABSTRACT:
          case METHOD_FINAL:
          case METHOD_NATIVE:
          case METHOD_SYNCHRONIZED:
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[27] = jj_gen;
            break label_15;
        }
        MethodModifier();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_CLASS_PATTERN:
          returnType = jj_consume_token(METHOD_CLASS_PATTERN);
          break;
        case METHOD_ARRAY_CLASS_PATTERN:
          returnType = jj_consume_token(METHOD_ARRAY_CLASS_PATTERN);
          break;
        default:
          jj_la1[28] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtn000.setReturnTypePattern(returnType.image);
      name = jj_consume_token(METHOD_CLASS_PATTERN);
      jjtn000.setFullNamePattern(name.image);
      Parameters();
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Constructor pattern.
   *
   * @TODO: split class name and constructor name ('new').
   * @TODO: handle '+'.
   * @TODO: put declaring class in a different node.
   */
  static final public void ConstructorPattern() throws ParseException {
    /*@bgen(jjtree) ConstructorPattern */
    ASTConstructorPattern jjtn000 = new ASTConstructorPattern(JJTCONSTRUCTORPATTERN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token name;
    try {
      label_16:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_PUBLIC:
          case METHOD_PROTECTED:
          case METHOD_PRIVATE:
          case METHOD_SYNCHRONIZED:
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[29] = jj_gen;
            break label_16;
        }
        ConstructorModifier();
      }
      name = jj_consume_token(METHOD_CLASS_PATTERN);
      if (!name.image.endsWith("new")) {
        {
          if (true) throw new RuntimeException("constructor pattern must have 'new' as method name");
        }
      }
      jjtn000.setFullNamePattern(name.image);
      Parameters();
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Field pattern.
   *
   * @TODO: split class name and field name.
   * @TODO: handle '+'.
   * @TODO: put field name, field type and declaring class in different nodes.
   */
  static final public void FieldPattern() throws ParseException {
    /*@bgen(jjtree) FieldPattern */
    ASTFieldPattern jjtn000 = new ASTFieldPattern(JJTFIELDPATTERN);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token type, name;
    try {
      label_17:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case FIELD_PRIVATE:
          case FIELD_PROTECTED:
          case FIELD_PUBLIC:
          case FIELD_STATIC:
          case FIELD_ABSTRACT:
          case FIELD_FINAL:
          case FIELD_TRANSIENT:
          case FIELD_NOT:
            ;
            break;
          default:
            jj_la1[30] = jj_gen;
            break label_17;
        }
        FieldModifier();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case FIELD_CLASS_PATTERN:
          type = jj_consume_token(FIELD_CLASS_PATTERN);
          break;
        case FIELD_ARRAY_CLASS_PATTERN:
          type = jj_consume_token(FIELD_ARRAY_CLASS_PATTERN);
          break;
        default:
          jj_la1[31] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtn000.setFieldTypePattern(type.image);
      name = jj_consume_token(FIELD_CLASS_PATTERN);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setFullNamePattern(name.image);
    } catch (Throwable jjte000) {
      if (jjtc000) {
        jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        jjtree.popNode();
      }
      if (jjte000 instanceof RuntimeException) {
        {
          if (true) throw (RuntimeException) jjte000;
        }
      }
      if (jjte000 instanceof ParseException) {
        {
          if (true) throw (ParseException) jjte000;
        }
      }
      {
        if (true) throw (Error) jjte000;
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Parameters.
   */
  static final public void Parameters() throws ParseException {
    jj_consume_token(METHOD_PARAMETER_START);
    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
      case EAGER_WILDCARD:
      case METHOD_CLASS_PATTERN:
      case METHOD_ARRAY_CLASS_PATTERN:
        Parameter();
        label_18:
        while (true) {
          switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case COMMA:
              ;
              break;
            default:
              jj_la1[32] = jj_gen;
              break label_18;
          }
          jj_consume_token(COMMA);
          Parameter();
        }
        break;
      default:
        jj_la1[33] = jj_gen;
        ;
    }
    jj_consume_token(METHOD_PARAMETER_END);
  }

  /**
   * Parameter pattern.
   */
  static final public void Parameter() throws ParseException {
    /*@bgen(jjtree) Parameter */
    ASTParameter jjtn000 = new ASTParameter(JJTPARAMETER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token parameter;
    try {
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_CLASS_PATTERN:
          parameter = jj_consume_token(METHOD_CLASS_PATTERN);
          break;
        case METHOD_ARRAY_CLASS_PATTERN:
          parameter = jj_consume_token(METHOD_ARRAY_CLASS_PATTERN);
          break;
        case EAGER_WILDCARD:
          parameter = jj_consume_token(EAGER_WILDCARD);
          break;
        default:
          jj_la1[34] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setTypePattern(parameter.image);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * ArgsParameters.
   */
  static final public void ArgsParameters() throws ParseException {
    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
      case EAGER_WILDCARD:
      case ARG_PATTERN:
      case ARG_ARRAY_PATTERN:
        ArgParameter();
        break;
      default:
        jj_la1[35] = jj_gen;
        ;
    }
    label_19:
    while (true) {
      if (jj_2_18(2)) {
        ;
      } else {
        break label_19;
      }
      jj_consume_token(COMMA);
      ArgsParameters();
    }
  }

  /**
   * ArgParameter.
   */
  static final public void ArgParameter() throws ParseException {
    /*@bgen(jjtree) ArgParameter */
    ASTArgParameter jjtn000 = new ASTArgParameter(JJTARGPARAMETER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token t;
    try {
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case ARG_PATTERN:
          t = jj_consume_token(ARG_PATTERN);
          break;
        case ARG_ARRAY_PATTERN:
          t = jj_consume_token(ARG_ARRAY_PATTERN);
          break;
        case EAGER_WILDCARD:
          t = jj_consume_token(EAGER_WILDCARD);
          break;
        default:
          jj_la1[36] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setTypePattern(t.image);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Class annotation.
   */
  static final public void ClassAttribute() throws ParseException {
    /*@bgen(jjtree) Attribute */
    ASTAttribute jjtn000 = new ASTAttribute(JJTATTRIBUTE);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token annotation;
    try {
      label_20:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case CLASS_NOT:
            ;
            break;
          default:
            jj_la1[37] = jj_gen;
            break label_20;
        }
        jj_consume_token(CLASS_NOT);
        jjtn000.toggleNot();
      }
      annotation = jj_consume_token(CLASS_ATTRIBUTE);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setName(annotation.image);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Method annotation.
   */
  static final public void MethodAttribute() throws ParseException {
    /*@bgen(jjtree) Attribute */
    ASTAttribute jjtn000 = new ASTAttribute(JJTATTRIBUTE);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token annotation;
    try {
      label_21:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[38] = jj_gen;
            break label_21;
        }
        jj_consume_token(METHOD_NOT);
        jjtn000.toggleNot();
      }
      annotation = jj_consume_token(METHOD_ANNOTATION);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setName(annotation.image);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Field annotation.
   */
  static final public void FieldAttribute() throws ParseException {
    /*@bgen(jjtree) Attribute */
    ASTAttribute jjtn000 = new ASTAttribute(JJTATTRIBUTE);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    Token annotation;
    try {
      label_22:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case FIELD_NOT:
            ;
            break;
          default:
            jj_la1[39] = jj_gen;
            break label_22;
        }
        jj_consume_token(FIELD_NOT);
        jjtn000.toggleNot();
      }
      annotation = jj_consume_token(FIELD_ANNOTATION);
      jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      jjtn000.setName(annotation.image);
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Class modifier.
   */
  static final public void ClassModifier() throws ParseException {
    /*@bgen(jjtree) Modifier */
    ASTModifier jjtn000 = new ASTModifier(JJTMODIFIER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      label_23:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case CLASS_NOT:
            ;
            break;
          default:
            jj_la1[40] = jj_gen;
            break label_23;
        }
        jj_consume_token(CLASS_NOT);
        jjtn000.toggleNot();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case CLASS_PUBLIC:
          jj_consume_token(CLASS_PUBLIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PUBLIC);
          break;
        case CLASS_PROTECTED:
          jj_consume_token(CLASS_PROTECTED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PROTECTED);
          break;
        case CLASS_PRIVATE:
          jj_consume_token(CLASS_PRIVATE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PRIVATE);
          break;
        case CLASS_STATIC:
          jj_consume_token(CLASS_STATIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.STATIC);
          break;
        case CLASS_ABSTRACT:
          jj_consume_token(CLASS_ABSTRACT);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.ABSTRACT);
          break;
        case CLASS_FINAL:
          jj_consume_token(CLASS_FINAL);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.FINAL);
          break;
        default:
          jj_la1[41] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Method modifier.
   */
  static final public void StaticInitializationPatternModifier() throws ParseException {
    /*@bgen(jjtree) Modifier */
    ASTModifier jjtn000 = new ASTModifier(JJTMODIFIER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      label_24:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[42] = jj_gen;
            break label_24;
        }
        jj_consume_token(METHOD_NOT);
        jjtn000.toggleNot();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PUBLIC:
          jj_consume_token(METHOD_PUBLIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PUBLIC);
          break;
        case METHOD_PROTECTED:
          jj_consume_token(METHOD_PROTECTED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PROTECTED);
          break;
        case METHOD_PRIVATE:
          jj_consume_token(METHOD_PRIVATE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PRIVATE);
          break;
        case METHOD_STATIC:
          jj_consume_token(METHOD_STATIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.STATIC);
          break;
        case METHOD_ABSTRACT:
          jj_consume_token(METHOD_ABSTRACT);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.ABSTRACT);
          break;
        case METHOD_FINAL:
          jj_consume_token(METHOD_FINAL);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.FINAL);
          break;
        default:
          jj_la1[43] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Method modifier.
   */
  static final public void MethodModifier() throws ParseException {
    /*@bgen(jjtree) Modifier */
    ASTModifier jjtn000 = new ASTModifier(JJTMODIFIER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      label_25:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[44] = jj_gen;
            break label_25;
        }
        jj_consume_token(METHOD_NOT);
        jjtn000.toggleNot();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PUBLIC:
          jj_consume_token(METHOD_PUBLIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PUBLIC);
          break;
        case METHOD_PROTECTED:
          jj_consume_token(METHOD_PROTECTED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PROTECTED);
          break;
        case METHOD_PRIVATE:
          jj_consume_token(METHOD_PRIVATE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PRIVATE);
          break;
        case METHOD_STATIC:
          jj_consume_token(METHOD_STATIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.STATIC);
          break;
        case METHOD_ABSTRACT:
          jj_consume_token(METHOD_ABSTRACT);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.ABSTRACT);
          break;
        case METHOD_FINAL:
          jj_consume_token(METHOD_FINAL);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.FINAL);
          break;
        case METHOD_NATIVE:
          jj_consume_token(METHOD_NATIVE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.NATIVE);
          break;
        case METHOD_SYNCHRONIZED:
          jj_consume_token(METHOD_SYNCHRONIZED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.SYNCHRONIZED);
          break;
        default:
          jj_la1[45] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Constructor modifier.
   */
  static final public void ConstructorModifier() throws ParseException {
    /*@bgen(jjtree) Modifier */
    ASTModifier jjtn000 = new ASTModifier(JJTMODIFIER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      label_26:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case METHOD_NOT:
            ;
            break;
          default:
            jj_la1[46] = jj_gen;
            break label_26;
        }
        jj_consume_token(METHOD_NOT);
        jjtn000.toggleNot();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case METHOD_PUBLIC:
          jj_consume_token(METHOD_PUBLIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PUBLIC);
          break;
        case METHOD_PROTECTED:
          jj_consume_token(METHOD_PROTECTED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PROTECTED);
          break;
        case METHOD_PRIVATE:
          jj_consume_token(METHOD_PRIVATE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PRIVATE);
          break;
        case METHOD_SYNCHRONIZED:
          jj_consume_token(METHOD_SYNCHRONIZED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.SYNCHRONIZED);
          break;
        default:
          jj_la1[47] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  /**
   * Field modifier.
   */
  static final public void FieldModifier() throws ParseException {
    /*@bgen(jjtree) Modifier */
    ASTModifier jjtn000 = new ASTModifier(JJTMODIFIER);
    boolean jjtc000 = true;
    jjtree.openNodeScope(jjtn000);
    try {
      label_27:
      while (true) {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
          case FIELD_NOT:
            ;
            break;
          default:
            jj_la1[48] = jj_gen;
            break label_27;
        }
        jj_consume_token(FIELD_NOT);
        jjtn000.toggleNot();
      }
      switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case FIELD_PUBLIC:
          jj_consume_token(FIELD_PUBLIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PUBLIC);
          break;
        case FIELD_PROTECTED:
          jj_consume_token(FIELD_PROTECTED);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PROTECTED);
          break;
        case FIELD_PRIVATE:
          jj_consume_token(FIELD_PRIVATE);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.PRIVATE);
          break;
        case FIELD_STATIC:
          jj_consume_token(FIELD_STATIC);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.STATIC);
          break;
        case FIELD_ABSTRACT:
          jj_consume_token(FIELD_ABSTRACT);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.ABSTRACT);
          break;
        case FIELD_FINAL:
          jj_consume_token(FIELD_FINAL);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.FINAL);
          break;
        case FIELD_TRANSIENT:
          jj_consume_token(FIELD_TRANSIENT);
          jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          jjtn000.setModifier(Modifier.TRANSIENT);
          break;
        default:
          jj_la1[49] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
      }
    } finally {
      if (jjtc000) {
        jjtree.closeNodeScope(jjtn000, true);
      }
    }
  }

  static final private boolean jj_2_1(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_1();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(0, xla);
    }
  }

  static final private boolean jj_2_2(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_2();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(1, xla);
    }
  }

  static final private boolean jj_2_3(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_3();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(2, xla);
    }
  }

  static final private boolean jj_2_4(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_4();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(3, xla);
    }
  }

  static final private boolean jj_2_5(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_5();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(4, xla);
    }
  }

  static final private boolean jj_2_6(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_6();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(5, xla);
    }
  }

  static final private boolean jj_2_7(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_7();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(6, xla);
    }
  }

  static final private boolean jj_2_8(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_8();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(7, xla);
    }
  }

  static final private boolean jj_2_9(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_9();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(8, xla);
    }
  }

  static final private boolean jj_2_10(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_10();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(9, xla);
    }
  }

  static final private boolean jj_2_11(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_11();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(10, xla);
    }
  }

  static final private boolean jj_2_12(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_12();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(11, xla);
    }
  }

  static final private boolean jj_2_13(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_13();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(12, xla);
    }
  }

  static final private boolean jj_2_14(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_14();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(13, xla);
    }
  }

  static final private boolean jj_2_15(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_15();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(14, xla);
    }
  }

  static final private boolean jj_2_16(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_16();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(15, xla);
    }
  }

  static final private boolean jj_2_17(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_17();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(16, xla);
    }
  }

  static final private boolean jj_2_18(int xla) {
    jj_la = xla;
    jj_lastpos = jj_scanpos = token;
    try {
      return !jj_3_18();
    }
    catch (LookaheadSuccess ls) {
      return true;
    }
    finally {
      jj_save(17, xla);
    }
  }

  static final private boolean jj_3R_47() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_scan_token(82)) {
      jj_scanpos = xsp;
      if (jj_scan_token(83)) {
        jj_scanpos = xsp;
        if (jj_scan_token(7)) return true;
      }
    }
    return false;
  }

  static final private boolean jj_3R_77() {
    if (jj_scan_token(HAS_METHOD)) return true;
    return false;
  }

  static final private boolean jj_3_3() {
    if (jj_3R_30()) return true;
    return false;
  }

  static final private boolean jj_3_15() {
    if (jj_3R_30()) return true;
    return false;
  }

  static final private boolean jj_3R_46() {
    if (jj_3R_55()) return true;
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_56()) {
        jj_scanpos = xsp;
        break;
      }
    }
    return false;
  }

  static final private boolean jj_3R_75() {
    if (jj_scan_token(EXECUTION)) return true;
    return false;
  }

  static final private boolean jj_3R_41() {
    if (jj_3R_47()) return true;
    return false;
  }

  static final private boolean jj_3R_34() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_41()) jj_scanpos = xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_18()) {
        jj_scanpos = xsp;
        break;
      }
    }
    return false;
  }

  static final private boolean jj_3R_91() {
    if (jj_scan_token(ARGS)) return true;
    return false;
  }

  static final private boolean jj_3R_90() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_scan_token(27)) {
      jj_scanpos = xsp;
      if (jj_scan_token(28)) return true;
    }
    return false;
  }

  static final private boolean jj_3R_83() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_14()) {
      jj_scanpos = xsp;
      if (jj_3R_91()) return true;
    }
    return false;
  }

  static final private boolean jj_3_14() {
    if (jj_scan_token(ARGS)) return true;
    if (jj_scan_token(ARGS_END)) return true;
    return false;
  }

  static final private boolean jj_3R_55() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_scan_token(54)) {
      jj_scanpos = xsp;
      if (jj_scan_token(55)) {
        jj_scanpos = xsp;
        if (jj_scan_token(7)) return true;
      }
    }
    return false;
  }

  static final private boolean jj_3R_71() {
    if (jj_3R_88()) return true;
    return false;
  }

  static final private boolean jj_3R_70() {
    if (jj_3R_87()) return true;
    return false;
  }

  static final private boolean jj_3R_72() {
    if (jj_3R_89()) return true;
    return false;
  }

  static final private boolean jj_3R_69() {
    if (jj_3R_86()) return true;
    return false;
  }

  static final private boolean jj_3R_73() {
    if (jj_3R_90()) return true;
    return false;
  }

  static final private boolean jj_3R_87() {
    if (jj_scan_token(CFLOW_BELOW)) return true;
    return false;
  }

  static final private boolean jj_3R_68() {
    if (jj_3R_85()) return true;
    return false;
  }

  static final private boolean jj_3R_67() {
    if (jj_3R_84()) return true;
    return false;
  }

  static final private boolean jj_3R_66() {
    if (jj_3R_83()) return true;
    return false;
  }

  static final private boolean jj_3R_38() {
    if (jj_scan_token(METHOD_PARAMETER_START)) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_46()) jj_scanpos = xsp;
    if (jj_scan_token(METHOD_PARAMETER_END)) return true;
    return false;
  }

  static final private boolean jj_3R_65() {
    if (jj_3R_82()) return true;
    return false;
  }

  static final private boolean jj_3R_86() {
    if (jj_scan_token(CFLOW)) return true;
    return false;
  }

  static final private boolean jj_3R_64() {
    if (jj_3R_81()) return true;
    return false;
  }

  static final private boolean jj_3R_63() {
    if (jj_3R_80()) return true;
    return false;
  }

  static final private boolean jj_3R_44() {
    if (jj_3R_49()) return true;
    return false;
  }

  static final private boolean jj_3R_62() {
    if (jj_3R_79()) return true;
    return false;
  }

  static final private boolean jj_3R_61() {
    if (jj_3R_78()) return true;
    return false;
  }

  static final private boolean jj_3R_60() {
    if (jj_3R_77()) return true;
    return false;
  }

  static final private boolean jj_3R_59() {
    if (jj_3R_76()) return true;
    return false;
  }

  static final private boolean jj_3_13() {
    if (jj_3R_33()) return true;
    return false;
  }

  static final private boolean jj_3R_88() {
    if (jj_scan_token(STATIC_INITIALIZATION)) return true;
    return false;
  }

  static final private boolean jj_3R_58() {
    if (jj_3R_75()) return true;
    return false;
  }

  static final private boolean jj_3R_49() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_57()) {
      jj_scanpos = xsp;
      if (jj_3R_58()) {
        jj_scanpos = xsp;
        if (jj_3R_59()) {
          jj_scanpos = xsp;
          if (jj_3R_60()) {
            jj_scanpos = xsp;
            if (jj_3R_61()) {
              jj_scanpos = xsp;
              if (jj_3R_62()) {
                jj_scanpos = xsp;
                if (jj_3R_63()) {
                  jj_scanpos = xsp;
                  if (jj_3R_64()) {
                    jj_scanpos = xsp;
                    if (jj_3R_65()) {
                      jj_scanpos = xsp;
                      if (jj_3R_66()) {
                        jj_scanpos = xsp;
                        if (jj_3R_67()) {
                          jj_scanpos = xsp;
                          if (jj_3R_68()) {
                            jj_scanpos = xsp;
                            if (jj_3R_69()) {
                              jj_scanpos = xsp;
                              if (jj_3R_70()) {
                                jj_scanpos = xsp;
                                if (jj_3R_71()) {
                                  jj_scanpos = xsp;
                                  if (jj_3R_72()) {
                                    jj_scanpos = xsp;
                                    if (jj_3R_73()) return true;
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  static final private boolean jj_3R_57() {
    if (jj_3R_74()) return true;
    return false;
  }

  static final private boolean jj_3_11() {
    if (jj_3R_31()) return true;
    return false;
  }

  static final private boolean jj_3_12() {
    if (jj_3R_30()) return true;
    return false;
  }

  static final private boolean jj_3R_43() {
    if (jj_scan_token(86)) return true;
    return false;
  }

  static final private boolean jj_3_2() {
    if (jj_scan_token(OR)) return true;
    if (jj_3R_29()) return true;
    return false;
  }

  static final private boolean jj_3R_37() {
    if (jj_3R_45()) return true;
    return false;
  }

  static final private boolean jj_3R_31() {
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_37()) {
        jj_scanpos = xsp;
        break;
      }
    }
    if (jj_scan_token(METHOD_CLASS_PATTERN)) return true;
    if (jj_3R_38()) return true;
    return false;
  }

  static final private boolean jj_3R_48() {
    if (jj_scan_token(NOT)) return true;
    return false;
  }

  static final private boolean jj_3_1() {
    if (jj_scan_token(AND)) return true;
    if (jj_3R_28()) return true;
    return false;
  }

  static final private boolean jj_3R_42() {
    if (jj_3R_48()) return true;
    return false;
  }

  static final private boolean jj_3R_35() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_42()) {
      jj_scanpos = xsp;
      if (jj_3R_43()) {
        jj_scanpos = xsp;
        if (jj_3R_44()) return true;
      }
    }
    return false;
  }

  static final private boolean jj_3_10() {
    if (jj_3R_30()) return true;
    return false;
  }

  static final private boolean jj_3R_28() {
    if (jj_3R_35()) return true;
    return false;
  }

  static final private boolean jj_3R_29() {
    if (jj_3R_28()) return true;
    return false;
  }

  static final private boolean jj_3R_76() {
    if (jj_scan_token(WITHIN_CODE)) return true;
    return false;
  }

  static final private boolean jj_3_9() {
    if (jj_3R_33()) return true;
    return false;
  }

  static final private boolean jj_3R_81() {
    if (jj_scan_token(WITHIN)) return true;
    return false;
  }

  static final private boolean jj_3R_82() {
    if (jj_scan_token(HANDLER)) return true;
    return false;
  }

  static final private boolean jj_3R_89() {
    if (jj_scan_token(IF)) return true;
    return false;
  }

  static final private boolean jj_3_8() {
    if (jj_3R_32()) return true;
    return false;
  }

  static final private boolean jj_3R_39() {
    if (jj_scan_token(FIELD_NOT)) return true;
    return false;
  }

  static final private boolean jj_3R_32() {
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_39()) {
        jj_scanpos = xsp;
        break;
      }
    }
    if (jj_scan_token(FIELD_ANNOTATION)) return true;
    return false;
  }

  static final private boolean jj_3R_79() {
    if (jj_scan_token(GET)) return true;
    return false;
  }

  static final private boolean jj_3R_85() {
    if (jj_scan_token(THIS)) return true;
    return false;
  }

  static final private boolean jj_3_7() {
    if (jj_3R_32()) return true;
    return false;
  }

  static final private boolean jj_3R_78() {
    if (jj_scan_token(SET)) return true;
    return false;
  }

  static final private boolean jj_3_6() {
    if (jj_3R_31()) return true;
    return false;
  }

  static final private boolean jj_3R_36() {
    if (jj_scan_token(METHOD_NOT)) return true;
    return false;
  }

  static final private boolean jj_3R_30() {
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_36()) {
        jj_scanpos = xsp;
        break;
      }
    }
    if (jj_scan_token(METHOD_ANNOTATION)) return true;
    return false;
  }

  static final private boolean jj_3R_54() {
    if (jj_scan_token(METHOD_SYNCHRONIZED)) return true;
    return false;
  }

  static final private boolean jj_3R_84() {
    if (jj_scan_token(TARGET)) return true;
    return false;
  }

  static final private boolean jj_3R_53() {
    if (jj_scan_token(METHOD_PRIVATE)) return true;
    return false;
  }

  static final private boolean jj_3R_52() {
    if (jj_scan_token(METHOD_PROTECTED)) return true;
    return false;
  }

  static final private boolean jj_3R_51() {
    if (jj_scan_token(METHOD_PUBLIC)) return true;
    return false;
  }

  static final private boolean jj_3_5() {
    if (jj_3R_30()) return true;
    return false;
  }

  static final private boolean jj_3R_74() {
    if (jj_scan_token(CALL)) return true;
    return false;
  }

  static final private boolean jj_3_17() {
    if (jj_3R_32()) return true;
    return false;
  }

  static final private boolean jj_3R_40() {
    if (jj_scan_token(CLASS_NOT)) return true;
    return false;
  }

  static final private boolean jj_3R_33() {
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_40()) {
        jj_scanpos = xsp;
        break;
      }
    }
    if (jj_scan_token(CLASS_ATTRIBUTE)) return true;
    return false;
  }

  static final private boolean jj_3R_50() {
    if (jj_scan_token(METHOD_NOT)) return true;
    return false;
  }

  static final private boolean jj_3R_80() {
    if (jj_scan_token(HAS_FIELD)) return true;
    return false;
  }

  static final private boolean jj_3R_45() {
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3R_50()) {
        jj_scanpos = xsp;
        break;
      }
    }
    xsp = jj_scanpos;
    if (jj_3R_51()) {
      jj_scanpos = xsp;
      if (jj_3R_52()) {
        jj_scanpos = xsp;
        if (jj_3R_53()) {
          jj_scanpos = xsp;
          if (jj_3R_54()) return true;
        }
      }
    }
    return false;
  }

  static final private boolean jj_3_4() {
    if (jj_3R_31()) return true;
    return false;
  }

  static final private boolean jj_3_16() {
    if (jj_3R_31()) return true;
    return false;
  }

  static final private boolean jj_3_18() {
    if (jj_scan_token(COMMA)) return true;
    if (jj_3R_34()) return true;
    return false;
  }

  static final private boolean jj_3R_56() {
    if (jj_scan_token(COMMA)) return true;
    return false;
  }

  static private boolean jj_initialized_once = false;
  static public ExpressionParserTokenManager token_source;
  static SimpleCharStream jj_input_stream;
  static public Token token, jj_nt;
  static private int jj_ntk;
  static private Token jj_scanpos, jj_lastpos;
  static private int jj_la;
  static public boolean lookingAhead = false;
  static private boolean jj_semLA;
  static private int jj_gen;
  static final private int[] jj_la1 = new int[50];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static private int[] jj_la1_2;

  static {
    jj_la1_0();
    jj_la1_1();
    jj_la1_2();
  }

  private static void jj_la1_0() {
    jj_la1_0 = new int[]{0x1ffffc00, 0x1ffff800, 0x18000000, 0x88, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0xe0000080, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0xe0000080, 0x200000, 0x0, 0x0, 0x0, 0x0, 0xe0000000, 0x80, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x80, 0x80, 0x80, 0x80, 0x0, 0x0, 0x0, 0x0, 0xe0000000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,};
  }

  private static void jj_la1_1() {
    jj_la1_1 = new int[]{0x0, 0x0, 0x0, 0x0, 0xcbfc00, 0xcbfc00, 0x4cbfc00, 0xcbfc00, 0xcbfc00, 0x4cbfc00, 0xf0000000, 0xf0000000, 0x2f, 0xcbfc00, 0xcbfc00, 0x4cbfc00, 0x4dffc00, 0x48fc00, 0x8fc00, 0x2f, 0x0, 0xcbfc00, 0xcbfc00, 0x4cbfc00, 0xf0000000, 0xf, 0x20, 0xbfc00, 0xc00000, 0xa1c00, 0xf0000000, 0x0, 0x0, 0xc00000, 0xc00000, 0x0, 0x0, 0x8, 0x80000, 0x0, 0x8, 0x7, 0x80000, 0xfc00, 0x80000, 0x3fc00, 0x80000, 0x21c00, 0x0, 0xf0000000,};
  }

  private static void jj_la1_2() {
    jj_la1_2 = new int[]{0x400000, 0x0, 0x0, 0x2c0000, 0x0, 0x0, 0x800000, 0x0, 0x0, 0x800000, 0xcf, 0xcf, 0x0, 0x0, 0x0, 0x800000, 0x800000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x800000, 0xcf, 0x0, 0x0, 0x0, 0x0, 0x0, 0xf, 0xc0, 0x0, 0x0, 0x0, 0xc0000, 0xc0000, 0x0, 0x0, 0x8, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x7,};
  }

  static final private JJCalls[] jj_2_rtns = new JJCalls[18];
  static private boolean jj_rescan = false;
  static private int jj_gc = 0;

  public ExpressionParser(java.io.InputStream stream) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser.  You must");
      System.out.println("       either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ExpressionParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  static public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jjtree.reset();
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public ExpressionParser(java.io.Reader stream) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser.  You must");
      System.out.println("       either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ExpressionParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  static public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jjtree.reset();
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public ExpressionParser(ExpressionParserTokenManager tm) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser.  You must");
      System.out.println("       either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  @FindbugsSuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public void ReInit(ExpressionParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jjtree.reset();
    jj_gen = 0;
    for (int i = 0; i < 50; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  static final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static final class LookaheadSuccess extends java.lang.Error {
  }

  static final private LookaheadSuccess jj_ls = new LookaheadSuccess();

  static final private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0;
      Token tok = token;
      while (tok != null && tok != jj_scanpos) {
        i++;
        tok = tok.next;
      }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }

  static final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  static final public Token getToken(int index) {
    Token t = lookingAhead ? jj_scanpos : token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  static final private int jj_ntk() {
    if ((jj_nt = token.next) == null)
      return (jj_ntk = (token.next = token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  static private java.util.Vector jj_expentries = new java.util.Vector();
  static private int[] jj_expentry;
  static private int jj_kind = -1;
  static private int[] jj_lasttokens = new int[100];
  static private int jj_endpos;

  static private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      boolean exists = false;
      for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
        int[] oldentry = (int[]) (e.nextElement());
        if (oldentry.length == jj_expentry.length) {
          exists = true;
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              exists = false;
              break;
            }
          }
          if (exists) break;
        }
      }
      if (!exists) jj_expentries.addElement(jj_expentry);
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  static public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[88];
    for (int i = 0; i < 88; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 50; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1 << j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1 << j)) != 0) {
            la1tokens[32 + j] = true;
          }
          if ((jj_la1_2[i] & (1 << j)) != 0) {
            la1tokens[64 + j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 88; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[]) jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  static final public void enable_tracing() {
  }

  static final public void disable_tracing() {
  }

  static final private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 18; i++) {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg;
          jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0:
              jj_3_1();
              break;
            case 1:
              jj_3_2();
              break;
            case 2:
              jj_3_3();
              break;
            case 3:
              jj_3_4();
              break;
            case 4:
              jj_3_5();
              break;
            case 5:
              jj_3_6();
              break;
            case 6:
              jj_3_7();
              break;
            case 7:
              jj_3_8();
              break;
            case 8:
              jj_3_9();
              break;
            case 9:
              jj_3_10();
              break;
            case 10:
              jj_3_11();
              break;
            case 11:
              jj_3_12();
              break;
            case 12:
              jj_3_13();
              break;
            case 13:
              jj_3_14();
              break;
            case 14:
              jj_3_15();
              break;
            case 15:
              jj_3_16();
              break;
            case 16:
              jj_3_17();
              break;
            case 17:
              jj_3_18();
              break;
          }
        }
        p = p.next;
      } while (p != null);
    }
    jj_rescan = false;
  }

  static final private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) {
        p = p.next = new JJCalls();
        break;
      }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la;
    p.first = token;
    p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
