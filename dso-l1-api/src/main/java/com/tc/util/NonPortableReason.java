/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableRootContext;
import com.tc.text.NonPortableReasonFormatter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Encapsulate why something is non-portable and build nice error messages for printing when that occurs.
 */
public class NonPortableReason implements Serializable {

  private static final long serialVersionUID                    = 8149536931286184441L;

  public static final byte  UNDEFINED                           = 0x00;

  public static final byte  CLASS_NOT_ADAPTABLE                 = 0x01;
  public static final byte  SUPER_CLASS_NOT_ADAPTABLE           = 0x02;
  public static final byte  SUBCLASS_OF_LOGICALLY_MANAGED_CLASS = 0x03;
  public static final byte  CLASS_NOT_IN_BOOT_JAR               = 0x04;
  public static final byte  CLASS_NOT_INCLUDED_IN_CONFIG        = 0x05;
  public static final byte  SUPER_CLASS_NOT_INSTRUMENTED        = 0x06;
  public static final byte  TEST_REASON                         = 0x07;

  private static final byte LAST_DEFINED                        = 0x07;

  private final String      className;
  private final List        nonBootJarClasses;
  private final List        bootJarClasses;
  private final Collection  details;
  private final byte        reason;
  private transient String  detailedReason;
  private transient String  instructions;
  private String            message;
  private String            ultimateNonPortableFieldName;

  /**
   * @param clazz The class that is non-portable
   * @param reasonCode The reason why it is non-portable
   */
  public NonPortableReason(Class clazz, byte reasonCode) {
    this(clazz.getName(), reasonCode);
  }

  /**
   * @param className The class that is non-portable
   * @param reasonCode The reason why it is non-portable
   */
  public NonPortableReason(String className, byte reasonCode) {
    this.className = className;
    this.reason = reasonCode;
    this.details = new LinkedList();
    nonBootJarClasses = new LinkedList();
    bootJarClasses = new LinkedList();
  }

  /**
   * @return Class name
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return Detailed reason why something is non-portable
   */
  public synchronized String getDetailedReason() {
    if (detailedReason == null) {
      detailedReason = constructDetailedReason();
    }
    return detailedReason;
  }

  /**
   * @return Instructions on how to correct the problem
   */
  public synchronized String getInstructions() {
    if (instructions == null) {
      instructions = constructInstructions();
    }
    return instructions;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    checkSanity();
    out.defaultWriteObject();
  }

  /**
   * Add detail to the reason
   * 
   * @param label The label
   * @param value The value
   */
  public void addDetail(String label, String value) {
    details.add(new NonPortableDetail(label, value));
  }

  private String constructDetailedReason() {
    // XXX: This preamble thing is dumb. This messaging is constructed without the full context. I wasn't able to
    // refactor that problem away in time. --Orion 04/26/06
    boolean hasPreamble = message != null;
    StringBuffer sb = (message == null ? new StringBuffer() : new StringBuffer(message));

    switch (reason) {
      case CLASS_NOT_ADAPTABLE:
        if (hasPreamble) {
          sb.append(" This unshareable class is a");
        } else {
          sb.append("Attempted to share a");
        }
        sb.append(" JVM- or host machine-specific resource. Please ensure that instances of this class"
                  + " don't enter the shared object graph.");
        addDetail("Unshareable class", className);
        break;
      case SUPER_CLASS_NOT_ADAPTABLE:
        if (hasPreamble) {
          sb.append(" This unshareable class is a");
        } else {
          sb.append("Attempted to share an instance of a class that is a");
        }
        sb.append(" subclass of a JVM- or host machine-specific resource.");
        sb.append(" Please either modify the class hierarchy or ensure that instances of this class don't"
                  + " enter the shared object graph.");
        addDetail("Unshareable superclass names", getErroneousSuperClassNames());
        break;
      case SUBCLASS_OF_LOGICALLY_MANAGED_CLASS:
        if (hasPreamble) {
          sb.append(" This unshareable class");
        } else {
          sb.append("Attempted to share an instance of a class which");
        }
        sb.append(" has a logically-managed superclass.");
        sb.append(" Subclasses of logically-managed classes cannot be shared. Please either");
        sb.append(" modify the class hierarchy or ensure that instances of this class"
                  + " don't enter the shared object graph.");
        addDetail("Unshareable class", className);
        addDetail("Logically-managed superclass names", getErroneousSuperClassNames());
        break;
      case CLASS_NOT_IN_BOOT_JAR:
        if (hasPreamble) {
          sb.append(" This unshareable class");
        } else {
          sb.append("Attempted to share an instance of a class which");
        }
        if (!bootJarClasses.isEmpty()) {
          sb.append(" must be in the DSO boot jar.  It also has superclasses which must be in the DSO"
                    + " boot jar.  Please add all of these classes to the boot jar configuration and re-create"
                    + " the DSO boot jar.");
          List classes = new ArrayList();
          classes.addAll(bootJarClasses);
          classes.add(className);
          addDetail("Classes to add to boot jar", csvList(classes));
        } else {
          sb.append(" must be in the DSO boot jar. Please add this class to the boot jar configuration"
                    + " and re-create the DSO boot jar.");
          addDetail("Class to add to boot jar", className);
        }

        break;
      case SUPER_CLASS_NOT_INSTRUMENTED:
        if (hasPreamble) {
          sb.append(" This unshareable class has");
        } else {
          sb.append("Attempted to share an instance of a class which has");
        }
        boolean plural = (bootJarClasses.size() + nonBootJarClasses.size()) > 1;
        sb.append(plural ? " super-classes" : " a super-class");
        sb.append(" that" + (plural ? " are" : " is") + " uninstrumented."
                  + "  Subclasses of uninstrumented classes cannot be shared.");

        addDetail("Unshareable class", className);
        if (!bootJarClasses.isEmpty()) {
          addDetail("Classes to add to boot jar", csvList(bootJarClasses));
        }
        if (!nonBootJarClasses.isEmpty()) {
          addDetail("Classes to add to the <includes> configuration", csvList(nonBootJarClasses));
        }
        break;
      case CLASS_NOT_INCLUDED_IN_CONFIG:
        if (hasPreamble) {
          sb.append(" This unshareable class");
        } else {
          sb.append("Attempted to share an instance of a class which");
        }
        sb.append(" has not been included for sharing in the configuration.");
        if (!nonBootJarClasses.isEmpty()) {
          List classes = new LinkedList();
          classes.add(className);
          classes.addAll(nonBootJarClasses);
          addDetail("Non-included classes", csvList(classes));
        } else {
          addDetail("Non-included class", className);
        }
        if (!bootJarClasses.isEmpty()) {
          if (bootJarClasses.size() == 1) {
            addDetail("Class to add to boot jar", csvList(bootJarClasses));
          } else {
            addDetail("Classes to add to boot jar", csvList(bootJarClasses));
          }
        }
        break;
      case TEST_REASON:
        break;
      default:
        throw new AssertionError("Unknown reason: " + reason);
    }
    return sb.toString();
  }

  private String constructInstructions() {
    StringBuffer sb = new StringBuffer();
    switch (reason) {
      case CLASS_NOT_ADAPTABLE: {
        NonPortableDetail detail = findDetailByLabel(NonPortableRootContext.ROOT_NAME_LABEL);
        final boolean isRoot = detail != null;
        if (detail == null) {
          detail = findDetailByLabel(NonPortableFieldSetContext.FIELD_NAME_LABEL);
        }
        sb.append(Messages.classNotAdaptableInstructions(detail != null ? detail.getValue() : null, className, isRoot));
      }
        break;
      case SUPER_CLASS_NOT_ADAPTABLE: {
        NonPortableDetail detail = findDetailByLabel(NonPortableRootContext.ROOT_NAME_LABEL);
        final boolean isRoot = detail != null;
        if (detail == null) {
          detail = findDetailByLabel(NonPortableFieldSetContext.FIELD_NAME_LABEL);
        }
        if (detail == null) {
          detail = findDetailByLabel("Referring field");
        }
        sb.append(Messages.superClassNotAdaptableInstructions(detail != null ? detail.getValue() : null, className,
                                                              getErroneousSuperClassNames(), isRoot));
      }
        break;
      case SUBCLASS_OF_LOGICALLY_MANAGED_CLASS: {
        NonPortableDetail detail = findDetailByLabel(NonPortableRootContext.ROOT_NAME_LABEL);
        final boolean isRoot = detail != null;
        if (detail == null) {
          detail = findDetailByLabel(NonPortableFieldSetContext.FIELD_NAME_LABEL);
        }
        sb.append(Messages
            .subclassOfLogicallyManagedClassInstructions(detail != null ? detail.getValue() : null, className,
                                                         getErroneousSuperClassNames(), isRoot));
      }
        break;
      case CLASS_NOT_IN_BOOT_JAR:
        List classes = new LinkedList();
        classes.addAll(bootJarClasses);
        classes.add(className);
        sb.append(Messages.classNotInBootJarInstructions(classes));
        break;
      case CLASS_NOT_INCLUDED_IN_CONFIG:
        List normalClasses = new LinkedList();
        normalClasses.add(className);
        if (nonBootJarClasses != null) {
          normalClasses.addAll(nonBootJarClasses);
        }
        sb.append(Messages.classNotIncludedInConfigInstructions(normalClasses, bootJarClasses));
        break;
      case SUPER_CLASS_NOT_INSTRUMENTED:
        sb.append(Messages.superClassNotInstrumentedInstructions(nonBootJarClasses, bootJarClasses));
        break;
      case TEST_REASON:
        sb.append("instructions");
        break;
    }
    return sb.toString();
  }

  /**
   * Check whether this reason knows the field name referring to the non-portable object.
   * 
   * @return True if has field name
   */
  public boolean hasUltimateNonPortableFieldName() {
    return ultimateNonPortableFieldName != null;
  }

  /**
   * Set the name of the field holding the nonportable object.
   * 
   * @param name Name of the field
   */
  public void setUltimateNonPortableFieldName(String name) {
    addDetail("Referring field", name);
    ultimateNonPortableFieldName = name;
  }

  /**
   * @return the field holding the non-portable object.
   */
  public String getUltimateNonPortableFieldName() {
    return ultimateNonPortableFieldName;
  }

  private String getErroneousSuperClassNames() {
    Collection supers = new LinkedList(nonBootJarClasses);
    supers.addAll(bootJarClasses);
    return csvList(supers);
  }

  private static String csvList(Collection list) {
    StringBuffer sb = new StringBuffer();
    for (Iterator i = list.iterator(); i.hasNext();) {
      sb.append(i.next());
      if (i.hasNext()) {
        sb.append(", ");
      }
    }

    return sb.toString();
  }

  private void checkSanity() {
    if (reason <= UNDEFINED || reason > LAST_DEFINED) {
      // setReason() called with wrong values.
      throw new AssertionError(
                               "Please specify the reason for Non-portability by calling setReason() with one of the defined reasons.");
    }
    if ((reason == SUBCLASS_OF_LOGICALLY_MANAGED_CLASS || reason == SUPER_CLASS_NOT_ADAPTABLE || reason == SUPER_CLASS_NOT_INSTRUMENTED)
        && ((nonBootJarClasses.size() == 0) && (bootJarClasses.size() == 0))) {
      // addErroneousSuperClass() need to be called.
      throw new AssertionError("Please add erroneous super classes by calling addErroneousSuperClass()");
    }
  }

  /**
   * @return Reason code
   */
  public byte getReason() {
    return reason;
  }

  /**
   * Add erroneous super class
   * 
   * @param superClass Super class that is non-portable
   */
  public void addErroneousSuperClass(Class superClass) {
    if (superClass.getClassLoader() == null) {
      bootJarClasses.add(superClass.getName());
    } else {
      nonBootJarClasses.add(superClass.getName());
    }
  }

  /**
   * @return All erroneous super classes not in the boot jar
   */
  public List getErroneousSuperClasses() {
    return nonBootJarClasses;
  }

  /**
   * @return All erroneous super classes in the boot jar
   */
  public List getErroneousBootJarSuperClasses() {
    return bootJarClasses;
  }

  public String toString() {
    return getDetailedReason();
  }

  /**
   * @param msg The message
   */
  public void setMessage(String msg) {
    message = msg;
  }

  /**
   * @return The message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Accept a formatter for message formatting. This method will walk the reason text, details, and instructions.
   * 
   * @param formatter Formatter to help formatting the reason
   */
  public void accept(NonPortableReasonFormatter formatter) {
    formatter.formatReasonText(getDetailedReason());
    for (Iterator i = details.iterator(); i.hasNext();) {
      formatter.formatDetail((NonPortableDetail) i.next());
    }
    formatter.formatInstructionsText(getInstructions());
  }

  private NonPortableDetail findDetailByLabel(final String label) {
    for (Iterator pos = details.iterator(); pos.hasNext();) {
      NonPortableDetail detail = (NonPortableDetail) pos.next();
      if (label.equals(detail.getLabel())) { return detail; }
    }
    return null;
  }

  private static final class Messages {

    private static final ResourceBundle rb                                                         = ResourceBundle
                                                                                                       .getBundle(NonPortableReason.class
                                                                                                           .getName());

    private static final String         CLASS_NOT_ADAPTABLE_ROOT_INSTRUCTIONS_KEY                  = "classNotAdaptable.root.instructions";
    private static final String         CLASS_NOT_ADAPTABLE_FIELD_INSTRUCTIONS_KEY                 = "classNotAdaptable.field.instructions";

    private static final String         SUPER_CLASS_NOT_ADAPTABLE_ROOT_INSTRUCTIONS_KEY            = "superClassNotAdaptable.root.instructions";
    private static final String         SUPER_CLASS_NOT_ADAPTABLE_FIELD_INSTRUCTIONS_KEY           = "superClassNotAdaptable.field.instructions";

    private static final String         SUBCLASS_OF_LOGICALLY_MANAGED_CLASS_ROOT_INSTRUCTIONS_KEY  = "logicallyManagedSuperClass.root.instructions";
    private static final String         SUBCLASS_OF_LOGICALLY_MANAGED_CLASS_FIELD_INSTRUCTIONS_KEY = "logicallyManagedSuperClass.field.instructions";

    private static final String         CLASS_NOT_IN_BOOT_JAR_INFO_KEY                             = "classNotInBootJar.info";
    private static final String         CLASS_NOT_IN_BOOT_JAR_CLASS_KEY                            = "classNotInBootJar.class";
    private static final String         CLASS_NOT_IN_BOOT_JAR_INSTRUCTIONS_KEY                     = "classNotInBootJar.instructions";

    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_INFO_KEY                      = "classNotIncludedInConfig.info";
    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_HEADER_KEY                    = "classNotIncludedInConfig.header";
    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_NON_BOOTJAR_CLASS_KEY         = "classNotIncludedInConfig.non-bootjar.class";
    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_NON_BOOTJAR_INSTRUCTIONS_KEY  = "classNotIncludedInConfig.non-bootjar.instructions";
    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_BOOTJAR_CLASS_KEY             = "classNotIncludedInConfig.bootjar.class";
    private static final String         CLASS_NOT_INCLUDED_IN_CONFIG_BOOTJAR_INSTRUCTIONS_KEY      = "classNotIncludedInConfig.bootjar.instructions";

    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_INFO_KEY                      = "superClassNotInstrumented.info";
    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_HEADER_KEY                    = "superClassNotInstrumented.header";
    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_NON_BOOTJAR_CLASS_KEY         = "superClassNotInstrumented.non-bootjar.class";
    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_NON_BOOTJAR_INSTRUCTIONS_KEY  = "superClassNotInstrumented.non-bootjar.instructions";
    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_BOOTJAR_CLASS_KEY             = "superClassNotInstrumented.bootjar.class";
    private static final String         SUPER_CLASS_NOT_INSTRUMENTED_BOOTJAR_INSTRUCTIONS_KEY      = "superClassNotInstrumented.bootjar.instructions";

    static String classNotAdaptableInstructions(String fieldName, String nonAdaptableClassName, boolean isRootField) {
      return MessageFormat.format(rb.getString(isRootField ? CLASS_NOT_ADAPTABLE_ROOT_INSTRUCTIONS_KEY
          : CLASS_NOT_ADAPTABLE_FIELD_INSTRUCTIONS_KEY), new Object[] { fieldName, nonAdaptableClassName });
    }

    static String superClassNotAdaptableInstructions(String fieldName, String nonAdaptableSubclass,
                                                     String nonAdaptableSuperClasses, boolean isRootField) {
      return MessageFormat.format(rb.getString(isRootField ? SUPER_CLASS_NOT_ADAPTABLE_ROOT_INSTRUCTIONS_KEY
          : SUPER_CLASS_NOT_ADAPTABLE_FIELD_INSTRUCTIONS_KEY), new Object[] { fieldName, nonAdaptableSubclass,
          nonAdaptableSuperClasses });
    }

    static String subclassOfLogicallyManagedClassInstructions(String fieldName, String nonAdaptableSubclass,
                                                              String logicallyManagedSuperClasses, boolean isRootField) {
      return MessageFormat.format(rb.getString(isRootField ? SUBCLASS_OF_LOGICALLY_MANAGED_CLASS_ROOT_INSTRUCTIONS_KEY
          : SUBCLASS_OF_LOGICALLY_MANAGED_CLASS_FIELD_INSTRUCTIONS_KEY), new Object[] { fieldName,
          nonAdaptableSubclass, logicallyManagedSuperClasses });
    }

    static String classNotInBootJarInstructions(List classes) {
      final StringBuffer instructions = new StringBuffer(MessageFormat.format(rb
          .getString(CLASS_NOT_IN_BOOT_JAR_INFO_KEY), (Object[]) null));

      final StringBuffer classesMsg = new StringBuffer();
      for (Iterator pos = classes.iterator(); pos.hasNext();) {
        classesMsg.append(MessageFormat.format(rb.getString(CLASS_NOT_IN_BOOT_JAR_CLASS_KEY),
                                               new Object[] { pos.next() }));
      }
      instructions.append(MessageFormat.format(rb.getString(CLASS_NOT_IN_BOOT_JAR_INSTRUCTIONS_KEY),
                                               new Object[] { classesMsg }));
      return instructions.toString();
    }

    static String classNotIncludedInConfigInstructions(List normalClassNames, List bootJarClassNames) {
      final StringBuffer instructions = new StringBuffer(MessageFormat.format(rb
          .getString(CLASS_NOT_INCLUDED_IN_CONFIG_INFO_KEY), (Object[]) null));
      instructions.append(MessageFormat.format(rb.getString(CLASS_NOT_INCLUDED_IN_CONFIG_HEADER_KEY), (Object[]) null));
      if (normalClassNames != null && !normalClassNames.isEmpty()) {
        final StringBuffer classList = new StringBuffer();
        for (Iterator pos = normalClassNames.iterator(); pos.hasNext();) {
          classList.append(MessageFormat.format(rb.getString(CLASS_NOT_INCLUDED_IN_CONFIG_NON_BOOTJAR_CLASS_KEY),
                                                new Object[] { pos.next() }));
        }
        instructions.append(MessageFormat.format(rb
            .getString(CLASS_NOT_INCLUDED_IN_CONFIG_NON_BOOTJAR_INSTRUCTIONS_KEY), new Object[] { classList }));
      }
      if (bootJarClassNames != null && !bootJarClassNames.isEmpty()) {
        final StringBuffer bootJarClassList = new StringBuffer();
        for (Iterator pos = bootJarClassNames.iterator(); pos.hasNext();) {
          bootJarClassList.append(MessageFormat.format(rb.getString(CLASS_NOT_INCLUDED_IN_CONFIG_BOOTJAR_CLASS_KEY),
                                                       new Object[] { pos.next() }));
        }
        instructions.append(MessageFormat.format(rb.getString(CLASS_NOT_INCLUDED_IN_CONFIG_BOOTJAR_INSTRUCTIONS_KEY),
                                                 new Object[] { bootJarClassList }));
      }
      return instructions.toString();
    }

    static String superClassNotInstrumentedInstructions(List normalClassNames, List bootJarClassNames) {
      final StringBuffer instructions = new StringBuffer(MessageFormat.format(rb
          .getString(SUPER_CLASS_NOT_INSTRUMENTED_INFO_KEY), (Object[]) null));
      instructions.append(MessageFormat.format(rb.getString(SUPER_CLASS_NOT_INSTRUMENTED_HEADER_KEY), (Object[]) null));
      if (normalClassNames != null && !normalClassNames.isEmpty()) {
        final StringBuffer classList = new StringBuffer();
        for (Iterator pos = normalClassNames.iterator(); pos.hasNext();) {
          classList.append(MessageFormat.format(rb.getString(SUPER_CLASS_NOT_INSTRUMENTED_NON_BOOTJAR_CLASS_KEY),
                                                new Object[] { pos.next() }));
        }
        instructions.append(MessageFormat.format(rb
            .getString(SUPER_CLASS_NOT_INSTRUMENTED_NON_BOOTJAR_INSTRUCTIONS_KEY), new Object[] { classList }));
      }
      if (bootJarClassNames != null && !bootJarClassNames.isEmpty()) {
        final StringBuffer bootJarClassList = new StringBuffer();
        for (Iterator pos = bootJarClassNames.iterator(); pos.hasNext();) {
          bootJarClassList.append(MessageFormat.format(rb.getString(SUPER_CLASS_NOT_INSTRUMENTED_BOOTJAR_CLASS_KEY),
                                                       new Object[] { pos.next() }));
        }
        instructions.append(MessageFormat.format(rb.getString(SUPER_CLASS_NOT_INSTRUMENTED_BOOTJAR_INSTRUCTIONS_KEY),
                                                 new Object[] { bootJarClassList }));
      }
      return instructions.toString();
    }

  }

}
