/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableObjectState;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.walker.MemberValue;
import com.tc.object.walker.ObjectGraphWalker;
import com.tc.object.walker.Visitor;
import com.tc.object.walker.WalkTest;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class WalkVisitor implements Visitor, WalkTest {

  private static final LiteralValues    literals = new LiteralValues();

  private final ClientObjectManager     objMgr;
  private final DSOClientConfigHelper   config;
  private final NonPortableEventContext context;
  private final DefaultTreeModel        treeModel;

  public WalkVisitor(ClientObjectManager objMgr, DSOClientConfigHelper config, Object root,
                     NonPortableEventContext context) {
    this.objMgr = objMgr;
    this.config = config;
    this.context = context;
    this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
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
    addField(value, 0);
  }

  static String getTypeName(Class type) {
    if (type.isArray()) {
      try {
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
      } catch (Throwable e) {/* FALLTHRU */
      }
    }
    return type.getName();
  }

  String createLabel(MemberValue value) {
    Field field = value.getSourceField();
    Object o = value.getValueObject();
    StringBuffer sb = new StringBuffer();
    String rep = null;

    if(value.isElement()) {
      sb.append("["+value.getIndex()+"] ");
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

      if (type.isArray() && type.getComponentType() == Character.TYPE) {
        rep = new String((char[]) o);
      }
    } else {
      sb.append(o != null ? getTypeName(o.getClass()) : "java.lang.Object");
    }

    if (o == null || rep != null || isLiteralInstance(o)) {
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
    MutableTreeNode parent = getParent(depth);

    if (field != null && context instanceof NonPortableFieldSetContext) {
      NonPortableFieldSetContext cntx = (NonPortableFieldSetContext) context;
      String fieldName = field.getDeclaringClass().getName() + "." + field.getName();

      if (fieldName.equals(cntx.getFieldName())) {
        Object fieldValue = cntx.getFieldValue();
        WalkVisitor wv = new WalkVisitor(objMgr, config, fieldValue, null);
        ObjectGraphWalker walker = new ObjectGraphWalker(fieldValue, wv, wv);
        //walker.setMaxDepth(1);
        walker.walk();
        DefaultMutableTreeNode node = wv.getRootNode();
        NonPortableObjectState state = (NonPortableObjectState) node.getUserObject();
        state.setLabel(fieldName);
        treeModel.insertNodeInto(node, parent, parent.getChildCount());
        return;
      }
    }

    NonPortableObjectState objectState = createObjectState(value);
    MutableTreeNode childNode = new DefaultMutableTreeNode(objectState);
    treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
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
      Class type = o.getClass();
      return type.isArray() ? type.getComponentType() : type;
    } else if (field != null) {
      Class type = field.getType();
      return type.isArray() ? type.getComponentType() : type;
    }

    return null;
  }
  
  private String getTypeName(MemberValue value) {
    Class type = getType(value);
    return type != null ? type.getName() : null;
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

    if (!isPortable && isSystemType) {
      ArrayList list = new ArrayList();
      Class type = getType(value);

      list.add(typeName);
      while (true) {
        Class parentType = type.getSuperclass();
        if (parentType != null && !parentType.equals(Object.class) && !isPreInstrumented(parentType)) {
          list.add(parentType.getName());
          type = parentType;
        } else {
          break;
        }
      }
      objectState.setRequiredBootTypes((String[]) list.toArray(new String[0]));
    }

    return objectState;
  }

  public void visitMapEntry(int index, int depth) {
    MutableTreeNode parent = getParent(depth);
    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode("mapEntry [" + index + "]");
    treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
  }

  public void visitRootObject(MemberValue value) {
    addRoot(value);
  }

  public void visitValue(MemberValue value, int depth) {
    if (!skipVisit(value)) {
      addField(value, depth);
    }
  }

  private boolean isNeverAdaptable(MemberValue value) {
    Object o = value.getValueObject();
    if (o != null) return config.isNeverAdaptable(JavaClassInfo.getClassInfo(o.getClass()));
    else {
      Field field = value.getSourceField();
      if (field != null) { return config.isNeverAdaptable(JavaClassInfo.getClassInfo(field.getType())); }
    }
    return false;
  }

  public boolean shouldTraverse(MemberValue value) {
    if (skipVisit(value) || value.isRepeated() || isNeverAdaptable(value) || isTransient(value)) {
      return false;
    }

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
      if (type.isArray() && type.getComponentType() == Character.TYPE) { return false; }
    }

    return !isLiteralInstance(value.getValueObject());
  }

  private boolean isTransient(MemberValue val) {
    Field f = val.getSourceField();
    if (f == null) { return false; }

    return config.isTransient(f.getModifiers(), JavaClassInfo.getClassInfo(f.getDeclaringClass()), f.getName());
  }

  private static boolean skipVisit(MemberValue value) {
    Field field = value.getSourceField();

    if (field != null) {
      return (field.getType().getName().startsWith("com.tc.") || field.getName().startsWith("$__tc_"));
    } else {
      Object o = value.getValueObject();
      if (o != null) {
        Class clas = o.getClass();
        return (clas.getName().startsWith("com.tc."));
      }
    }
    return true;
  }

  private boolean isPortable(MemberValue value) {
    Object valueObject = value.getValueObject();

    if (valueObject != null) return objMgr.isPortableInstance(valueObject);
    else {
      Field field = value.getSourceField();
      if (field != null) {
        Class type = field.getType();
        if(!type.isInterface()) {
          return objMgr.isPortableClass(type);
        }
      }
    }
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
      if(field != null) {
        return isPreInstrumented(field.getType());
      }
    }
    return false;
  }

  private boolean isSystemType(MemberValue value) {
    Object o = value.getValueObject();

    if (o != null) {
      return (o.getClass().getClassLoader() == null);
    } else {
      Field field = value.getSourceField();
      if(field != null) return (field.getType().getClassLoader() == null);
    }
    return false;
  }

  private boolean isLiteralInstance(Object obj) {
    if (obj == null) { return false; }
    int i = literals.valueFor(obj);
    return i == LiteralValues.BOOLEAN || i == LiteralValues.BYTE || i == LiteralValues.CHARACTER
           || i == LiteralValues.DOUBLE || i == LiteralValues.FLOAT || i == LiteralValues.INTEGER
           || i == LiteralValues.LONG || i == LiteralValues.ENUM || i == LiteralValues.STRING;
  }
}
