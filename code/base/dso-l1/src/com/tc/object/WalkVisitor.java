/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableObjectState;
import com.tc.object.appevent.NonPortableRootContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.walker.MemberValue;
import com.tc.object.walker.ObjectGraphWalker;
import com.tc.object.walker.Visitor;
import com.tc.object.walker.WalkTest;
import com.tc.util.NonPortableReason;

import java.lang.reflect.Field;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class WalkVisitor implements Visitor, WalkTest {

  private final ClientObjectManager     objMgr;
  private final DSOClientConfigHelper   config;
  private final ApplicationEventContext context;
  private final DefaultTreeModel        treeModel;
  private boolean                       skipTCTypes            = true;

  private static final String           MAX_WALK_DEPTH_PROP    = "org.terracotta.non-portable.max-walk-depth";
  private static final int              DEFAULT_MAX_WALK_DEPTH = -1;
  private static final Integer          maxWalkDepth           = Integer.getInteger(MAX_WALK_DEPTH_PROP,
                                                                                    DEFAULT_MAX_WALK_DEPTH);

  public WalkVisitor(ClientObjectManager objMgr, DSOClientConfigHelper config, ApplicationEventContext context) {
    this.objMgr = objMgr;
    this.config = config;
    this.context = context;
    this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
  }

  // this is for testing purposes; default=true
  public void setSkipTCTypes(boolean skipTCTypes) {
    this.skipTCTypes = skipTCTypes;
  }

  public DefaultTreeModel getTreeModel() {
    return treeModel;
  }

  public DefaultMutableTreeNode getRootNode() {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    return (DefaultMutableTreeNode) root.getChildAt(0);
  }

  MutableTreeNode getParent(int depth) {
    MutableTreeNode node = (MutableTreeNode) treeModel.getRoot();
    while (depth-- > 0) {
      node = (MutableTreeNode) node.getChildAt(node.getChildCount() - 1);
    }
    return node;
  }

  public void addRoot(MemberValue value) {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getParent(0);
    NonPortableObjectState objectState = createRootObjectState(value);
    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(objectState);
    addChild(parent, childNode);
  }

  static String getTypeName(Class type) {
    if (type.isArray()) {
      Class cl = type;
      int dimensions = 0;
      while (cl.isArray()) {
        dimensions++;
        cl = cl.getComponentType();
      }
      StringBuffer sb = new StringBuffer();
      sb.append(cl.getName());
      for (int i = 0; i < dimensions; i++) {
        sb.append("[]");
      }
      return sb.toString();
    }
    return type.getName();
  }

  String createLabel(MemberValue value) {
    Field field = value.getSourceField();
    Object o = value.getValueObject();
    StringBuffer sb = new StringBuffer();
    String rep = null;

    if (value.isElement()) {
      sb.append("[" + value.getIndex() + "] ");
    } else if (value.isMapKey()) {
      sb.append("key ");
    } else if (value.isMapValue()) {
      sb.append("value ");
    }

    if (field != null) {
      Class type = getType(value);

      sb.append(field.getDeclaringClass().getName());
      sb.append(".");
      sb.append(field.getName());
      sb.append(" (");
      sb.append(getTypeName(type));
      sb.append(")");

      if (type.isArray() && type.getComponentType() == Character.TYPE && o != null) {
        rep = new String((char[]) o);
      }
    } else {
      sb.append(o != null ? getTypeName(o.getClass()) : "java.lang.Object");
    }

    if (o == null || rep != null || isSimpleLiteralInstance(o)) {
      sb.append("=");
      if (rep != null) {
        sb.append(rep);
      } else {
        sb.append(o != null ? o.toString() : "null");
      }
    }

    return sb.toString();
  }

  public void addField(MemberValue value, int depth) {
    Field field = value.getSourceField();
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getParent(depth);

    if (field != null && context instanceof NonPortableFieldSetContext) {
      NonPortableFieldSetContext cntx = (NonPortableFieldSetContext) context;
      String fieldName = field.getDeclaringClass().getName() + "." + field.getName();

      if (fieldName.equals(cntx.getFieldName())) {
        Object fieldValue = cntx.getFieldValue();
        WalkVisitor wv = new WalkVisitor(objMgr, config, null);
        ObjectGraphWalker walker = new ObjectGraphWalker(fieldValue, wv, wv);
        walker.setMaxDepth(maxWalkDepth.intValue());
        walker.walk();
        DefaultMutableTreeNode node = wv.getRootNode();
        NonPortableObjectState state = (NonPortableObjectState) node.getUserObject();
        state.setFieldName(fieldName);
        state.setLabel(createLabel(value));
        addChild(parent, node);
        return;
      }
    }

    NonPortableObjectState objectState = createObjectState(value);
    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(objectState);
    addChild(parent, childNode);
  }

  private String getFieldName(MemberValue value) {
    Field field = value.getSourceField();

    if (field != null) { return field.getDeclaringClass().getName() + "." + field.getName(); }

    return null;
  }

  private Class getType(MemberValue value) {
    Field field = value.getSourceField();
    Object o = value.getValueObject();

    if (o != null) {
      return o.getClass();
    } else if (field != null) { return field.getType(); }

    return null;
  }

  private String getTypeName(MemberValue value) {
    Class type = getType(value);
    return type != null ? type.getName() : null;
  }

  private NonPortableObjectState createRootObjectState(MemberValue value) {
    String typeName = getTypeName(value);
    boolean isPortable = isPortable(value);
    boolean isTransient = isTransient(value);
    boolean neverPortable = isNeverAdaptable(value);
    boolean isPreInstrumented = isPreInstrumented(value);
    boolean isRepeated = value.isRepeated();
    boolean isSystemType = isSystemType(value);
    boolean isNull = value.getValueObject() == null;
    String fieldName = null;
    String label;

    if (context instanceof NonPortableRootContext) {
      fieldName = ((NonPortableRootContext) context).getFieldName();
    } else {
      fieldName = getFieldName(value);
    }
    label = fieldName != null ? fieldName + " (" + typeName + ")" : typeName;

    NonPortableObjectState objectState = new NonPortableObjectState(label, fieldName, typeName, isPortable,
                                                                    isTransient, neverPortable, isPreInstrumented,
                                                                    isRepeated, isSystemType, isNull);

    if (!isPortable) {
      NonPortableReason reason = config.getPortability().getNonPortableReason(getType(value));
      objectState.setNonPortableReason(reason);
      objectState.setExplaination(handleSpecialCases(value));
    }

    return objectState;
  }

  private NonPortableObjectState createObjectState(MemberValue value) {
    String fieldName = getFieldName(value);
    String typeName = getTypeName(value);
    boolean isPortable = isPortable(value);
    boolean isTransient = isTransient(value);
    boolean neverPortable = isNeverAdaptable(value);
    boolean isPreInstrumented = isPreInstrumented(value);
    boolean isRepeated = value.isRepeated();
    boolean isSystemType = isSystemType(value);
    boolean isNull = value.getValueObject() == null;

    NonPortableObjectState objectState = new NonPortableObjectState(createLabel(value), fieldName, typeName,
                                                                    isPortable, isTransient, neverPortable,
                                                                    isPreInstrumented, isRepeated, isSystemType, isNull);

    if (!isPortable) {
      NonPortableReason reason = config.getPortability().getNonPortableReason(getType(value));
      objectState.setNonPortableReason(reason);
      objectState.setExplaination(handleSpecialCases(value));
    }

    return objectState;
  }

  private String handleSpecialCases(MemberValue value) {
    // TODO: create specialized explainations for well-known types, such as java.util.logging.Logger.
    return null;
  }

  public void visitMapEntry(int index, int depth) {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getParent(depth);
    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode("mapEntry [" + index + "]");
    addChild(parent, childNode);
  }

  private void addChild(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
    parent.add(child);
  }

  public void visitRootObject(MemberValue value) {
    addRoot(value);
  }

  public void visitValue(MemberValue value, int depth) {
    if (!skipVisit(value)) {
      addField(value, depth);
    }
  }

  private boolean isNeverAdaptable(Class type) {
    while (!type.equals(Object.class)) {
      if (config.isNeverAdaptable(JavaClassInfo.getClassInfo(type))) return true;
      type = type.getSuperclass();
    }
    return false;
  }

  private boolean isNeverAdaptable(MemberValue value) {
    Object o = value.getValueObject();
    if (o != null) return isNeverAdaptable(o.getClass());
    return false;
  }

  public boolean shouldTraverse(MemberValue value) {
    if (skipVisit(value) || value.isRepeated() || isNeverAdaptable(value) || isTransient(value)) { return false; }

    if (!isPortable(value) && isSystemType(value)) return false;

    if (context instanceof NonPortableFieldSetContext) {
      NonPortableFieldSetContext cntx = (NonPortableFieldSetContext) context;
      Field field = value.getSourceField();
      if (field != null) {
        String fieldName = field.getDeclaringClass().getName() + "." + field.getName();
        if (fieldName.equals(cntx.getFieldName())) { return false; }
      }
    }

    Field field = value.getSourceField();
    if (field != null) {
      Class type = field.getType();
      if (type.isArray() && type.getComponentType().isPrimitive()) { return false; }
    }

    return !isLiteralInstance(value.getValueObject());
  }

  private boolean isTransient(MemberValue val) {
    Field f = val.getSourceField();
    if (f == null) { return false; }

    return config.isTransient(f.getModifiers(), JavaClassInfo.getClassInfo(f.getDeclaringClass()), f.getName());
  }

  public boolean includeFieldsForType(Class type) {
    return !config.isLogical(type.getName());
  }

  private boolean skipVisit(MemberValue value) {
    Field field = value.getSourceField();
    if (skipTCTypes && field != null) { return (field.getType().getName().startsWith("com.tc.")); }
    return false;
  }

  private boolean isPortable(MemberValue value) {
    Object valueObject = value.getValueObject();
    if (valueObject != null) return objMgr.isPortableInstance(valueObject);
    return true;
  }

  private boolean isPreInstrumented(Class type) {
    if (type != null) {
      TransparencyClassSpec spec = config.getSpec(type.getName());
      return spec != null && spec.isPreInstrumented();
    }
    return false;
  }

  private boolean isPreInstrumented(MemberValue value) {
    Object o = value.getValueObject();

    if (o != null) {
      return isPreInstrumented(o.getClass());
    } else {
      Field field = value.getSourceField();
      if (field != null) { return isPreInstrumented(field.getType()); }
    }
    return false;
  }

  private boolean isSystemType(MemberValue value) {
    Object o = value.getValueObject();

    if (o != null) {
      return (o.getClass().getClassLoader() == null);
    } else {
      Field field = value.getSourceField();
      if (field != null) return (field.getType().getClassLoader() == null);
    }
    return false;
  }

  private boolean isLiteralInstance(Object obj) {
    return LiteralValues.isLiteralInstance(obj);
  }

  private boolean isSimpleLiteralInstance(Object obj) {
    if (obj == null) { return false; }
    LiteralValues i = LiteralValues.valueFor(obj);
    return i != LiteralValues.OBJECT && i != LiteralValues.ARRAY && i != LiteralValues.JAVA_LANG_CLASS
           && i != LiteralValues.JAVA_LANG_CLASS_HOLDER && i != LiteralValues.JAVA_LANG_CLASSLOADER
           && i != LiteralValues.JAVA_LANG_CLASSLOADER_HOLDER;
  }
}
