/*******************************************************************************************
 * Copyright (c) Jonas Bonér, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.bytecode;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

/**
 * Abstractions for the different reader elements.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
 */
public class AnnotationElement implements Serializable {

    public static final String DEFAULT_VALUE_NAME = "value";

    /**
     * Enum for the different annotation element types.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static final class Type {
        public static final Type ANNOTATION = new Type("ANNOTATION");
        public static final Type ARRAY = new Type("ARRAY");
        public static final Type ENUM = new Type("ENUM");
        public static final Type TYPE = new Type("TYPE");
        public static final Type STRING = new Type("STRING");
        public static final Type LONG = new Type("LONG");
        public static final Type INTEGER = new Type("INTEGER");
        public static final Type SHORT = new Type("SHORT");
        public static final Type DOUBLE = new Type("DOUBLE");
        public static final Type FLOAT = new Type("FLOAT");
        public static final Type BYTE = new Type("BYTE");
        public static final Type BOOLEAN = new Type("BOOLEAN");
        public static final Type CHAR = new Type("CHAR");

        private final String m_name;

        private Type(final String name) {
            m_name = name;
        }

        public String toString() {
            return m_name;
        }
    }

    /**
     * Abstraction for the annotation element type.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class Annotation extends AnnotationElement implements NestedAnnotationElement {
        static final long serialVersionUID = 8769673036736880936L;

        private final String m_className;
        private final List m_elements = new ArrayList();

        public Annotation(final String className) {
            m_className = className;
        }

        public void addElement(final String name, final Object element) {
            m_elements.add(new AnnotationElement.NamedValue(name, element));
        }

        /**
         * @return the annotation class name, java formatted (dot)
         */
        public String getInterfaceName() {
            return m_className;
        }

        public List getElements() {
            return m_elements;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            for (Iterator it = m_elements.iterator(); it.hasNext();) {
                NamedValue namedValue = (NamedValue)it.next();
                buf.append(namedValue.toString());
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            return buf.toString();
        }

        /**
         * Add the given element if not already present ie default value
         *
         * @param defaultedElement
         */
        public void mergeDefaultedElement(NamedValue defaultedElement) {
            for (Iterator iterator = m_elements.iterator(); iterator.hasNext();) {
                NamedValue namedValue = (NamedValue) iterator.next();
                if (namedValue.getName().equals(defaultedElement.getName())) {
                    return;// value is present
                }
            }
            m_elements.add(defaultedElement);
        }
    }

    /**
     * Abstraction for the array element type.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class Array extends AnnotationElement implements NestedAnnotationElement {
        static final long serialVersionUID = -6792525450471409048L;

        private final List m_elements = new ArrayList();

        public void addElement(final String name, final Object element) {
            m_elements.add(new AnnotationElement.NamedValue(DEFAULT_VALUE_NAME, element));
        }

        public List getElements() {
            return m_elements;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer("[");
            for (Iterator it = m_elements.iterator(); it.hasNext();) {
                NamedValue namedValue = (NamedValue)it.next();
                buf.append(namedValue.toString());
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            buf.append(']');
            return buf.toString();
        }
    }

    /**
     * Abstraction for the named value type.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class NamedValue extends AnnotationElement {
        static final long serialVersionUID = 4284696449802391088L;

        private final String m_name;
        private final Object m_value;
        private final Type m_type;
        private boolean m_isResolved = false;
        private Object m_resolvedValue;

        public NamedValue(final String name, final Object value) {
            if (name == null) {
                m_name = DEFAULT_VALUE_NAME;
            } else {
                m_name = name;
            }
            m_value = value;
            if (value instanceof Enum) {
                m_type = Type.ENUM;
            } else if (value instanceof Byte) {
                m_type = Type.BYTE;
            } else if (value instanceof Boolean) {
                m_type = Type.BOOLEAN;
            } else if (value instanceof Character) {
                m_type = Type.CHAR;
            } else if (value instanceof Short) {
                m_type = Type.SHORT;
            } else if (value instanceof Integer) {
                m_type = Type.INTEGER;
            } else if (value instanceof Long) {
                m_type = Type.LONG;
            } else if (value instanceof Float) {
                m_type = Type.FLOAT;
            } else if (value instanceof Double) {
                m_type = Type.DOUBLE;
            } else if (value instanceof String) {
                m_type = Type.STRING;
            } else if (value instanceof com.tc.asm.Type) {
                m_type = Type.TYPE;
            } else if (value instanceof Array) {
                m_type = Type.ARRAY;
            } else if (value instanceof Annotation) {
                m_type = Type.ANNOTATION;
            } else {
                throw new IllegalArgumentException(
                        "not valid type for named value in annotation [" + value.toString() + "]"
                );
            }
        }

        public String getName() {
            return m_name;
        }

        public Object getValue() {
            return m_value;
        }

        public Type getType() {
            return m_type;
        }

        public void setResolvedValue(final Object value) {
            m_isResolved = true;
            m_resolvedValue = value;
        }

        public boolean isResolved() {
            return m_isResolved;
        }

        public Object getResolvedValue() {
            return m_resolvedValue;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (!m_name.equals(DEFAULT_VALUE_NAME)) {
                buf.append(m_name);
                buf.append('=');
            }
            if (m_type.equals(Type.TYPE)) {
                buf.append(((com.tc.asm.Type)m_value).getClassName()).append(".class");
            } else {
                buf.append(m_value);
            }
            return buf.toString();
        }
    }

    /**
     * Abstraction for the enum (Java 5 enum) element type.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class Enum extends AnnotationElement {
        static final long serialVersionUID = 1136400223420236391L;

        private final String m_desc;
        private final String m_value;

        public Enum(final String desc, final String value) {
            m_desc = desc;
            m_value = value;
        }

        public String getDesc() {
            return m_desc;
        }

        public String getValue() {
            return m_value;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(m_desc.substring(1, m_desc.length() - 1).replace('/', '.'));
            buf.append('.');
            buf.append(m_value);
            return buf.toString();
        }
    }

    /**
     * Interface for the nested annotation element type. Composite pattern.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static interface NestedAnnotationElement {
        void addElement(String name, Object element);

        List getElements();
    }
}
