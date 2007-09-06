/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.text.NonPortableReasonFormatter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class NonPortableReason implements Serializable {

  private static final long serialVersionUID                    = 8149536931286184441L;

  public static final byte         UNDEFINED                           = 0x00;

  public static final byte  CLASS_NOT_ADAPTABLE                 = 0x01;
  public static final byte  SUPER_CLASS_NOT_ADAPTABLE           = 0x02;
  public static final byte  SUBCLASS_OF_LOGICALLY_MANAGED_CLASS = 0x03;
  public static final byte  CLASS_NOT_IN_BOOT_JAR               = 0x04;
  public static final byte  CLASS_NOT_INCLUDED_IN_CONFIG        = 0x05;
  public static final byte  SUPER_CLASS_NOT_INSTRUMENTED        = 0x06;
  public static final byte  TEST_REASON                         = 0x07;

  private static final byte LAST_DEFINED                        = 0x07;

  private final String      className;
  private final List        nonBootJarClasses                   = new ArrayList();
  private final List        bootJarClasses                      = new ArrayList();
  private final Collection  details;
  private final byte        reason;
  private transient String  detailedReason;
  private transient String  instructions;
  private String            message;
  private String            ultimateNonPortableFieldName;

  public NonPortableReason(Class clazz, byte reasonCode) {
    this.reason = reasonCode;
    this.className = clazz.getName();
    this.details = new LinkedList();
  }

  public NonPortableReason(String className, byte reasonCode) {
    this.className = className;
    this.reason = reasonCode;
    this.details = new LinkedList();
  }

  public String getClassName() {
    return className;
  }

  public synchronized String getDetailedReason() {
    if (detailedReason == null) {
      detailedReason = constructDetailedReason();
    }
    return detailedReason;
  }

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

  public void addDetail(String label, String value) {
    this.details.add(new NonPortableDetail(label, value));
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
        if (!this.bootJarClasses.isEmpty()) {
          sb.append(" must be in the DSO boot jar.  It also has superclasses which must be in the DSO"
                    + " boot jar.  Please add all of these classes to the boot jar configuration and re-create"
                    + " the DSO boot jar.");
          List classes = new ArrayList();
          classes.addAll(bootJarClasses);
          classes.add(this.className);
          addDetail("Classes to add to boot jar", csvList(classes));
        } else {
          sb.append(" must be in the DSO boot jar. Please add this class to the boot jar configuration"
                    + " and re-create the DSO boot jar.");
          addDetail("Class to add to boot jar", this.className);
        }

        break;
      case SUPER_CLASS_NOT_INSTRUMENTED:
        if (hasPreamble) {
          sb.append(" This unshareable class");
        } else {
          sb.append("Attempted to share an instance of a class which has");
        }
        boolean plural = (this.bootJarClasses.size() + this.nonBootJarClasses.size()) > 1;
        sb.append(plural ? " super-classes" : " a super-class");
        sb.append(" that" + (plural ? " are" : " is") + " uninstrumented." +
            "  Subclasses of uninstrumented classes cannot be shared.");
        if (! this.bootJarClasses.isEmpty()) {
          sb.append("  Please");
          if (this.bootJarClasses.size() > 1) {
            sb.append(" add the relevant super-classes to the boot jar configuration and re-create" +
                " the DSO boot jar.");
          }else {
            sb.append(" add the relevant super-class to the boot jar configuration and re-create" +
                " the DSO boot jar.");
          }
        }
        if (! this.nonBootJarClasses.isEmpty()) {
          sb.append("  Please");
          if (this.nonBootJarClasses.size() > 1) {
            sb.append(" add the relevant super-classes to the <includes> section of the configuration file.");
          } else {
            sb.append(" add the relevant super-class to the <includes> section of the configuration file.");
          }
        }

        addDetail("Unshareable class", className);
        if (! this.bootJarClasses.isEmpty()) {
          addDetail("Classes to add to boot jar", csvList(bootJarClasses));
        }
        if (! this.nonBootJarClasses.isEmpty()) {
          addDetail("Classes to add to the <includes> configuration", csvList(nonBootJarClasses));
        }
        break;
      case CLASS_NOT_INCLUDED_IN_CONFIG:
        if (hasPreamble) {
          sb.append(" This unshareable class");
        } else {
          sb.append("Attempted to share an instance of a class which");
        }
        sb.append(" has not been included for sharing in the configuration. Please add this class to the <includes>");
        sb.append(" section of the configuration file.");
        if (!this.nonBootJarClasses.isEmpty()) {
          if (this.nonBootJarClasses.size() == 1) {
            sb.append(" This class also has a super-class that has not been included for sharing in"
                      + " the configuration.  Please add this class to the <includes> section in the"
                      + " configuration file also.");
          } else {
            sb.append(" This class also has super-classes that have not been included for sharing in"
                      + " the configuration.  Please add these classes to the <includes> section of"
                      + " the configuration file also.");
          }
          List classes = new LinkedList();
          classes.add(className);
          classes.addAll(this.nonBootJarClasses);
          addDetail("Non-included classes", csvList(classes));
        } else {
          addDetail("Non-included class", className);
        }

        if (!this.bootJarClasses.isEmpty()) {
          if (this.bootJarClasses.size() == 1) {
            sb.append(" This class also has a super-class that must be in the DSO boot jar."
                      + " Please add this class to the boot jar configuration");
            addDetail("Class to add to boot jar", csvList(bootJarClasses));
          } else {
            sb.append(" This class also has super-classes that must be in the DSO boot jar."
                      + " Please add these classes to the boot jar configuration");
            addDetail("Classes to add to boot jar", csvList(bootJarClasses));
          }
          sb.append(" and re-create the DSO boot jar.");
        }

        break;
      case TEST_REASON:
        break;
      default:
        throw new AssertionError("Unknown reason: " + reason);
    }    
    
    sb.append("\n\nFor more information on this issue, please visit our Troubleshooting Guide at:\n");    
    sb.append("http://terracotta.org/kit/troubleshooting\n");
    
    return sb.toString();
  }

  private String constructInstructions() {
    StringBuffer sb = new StringBuffer();

    switch (reason) {
      case CLASS_NOT_IN_BOOT_JAR:
        List classes = new ArrayList();
        classes.addAll(bootJarClasses);
        classes.add(this.className);
        
        sb.append("\nTypical steps to resolve this are:\n\n");
        sb.append("* edit your tc-config.xml file\n");
        sb.append("* locate the <dso> tag\n");
        sb.append("* add this snippet inside the tag\n\n");
        sb.append("  <additional-boot-jar-classes>\n");
        for (Iterator i = classes.iterator(); i.hasNext();) {
          sb.append("    <include>");
          sb.append(i.next());
          sb.append("</include>\n");
        }
        sb.append("  </additional-boot-jar-classes>\n\n");
        sb.append("* if there's already an <additional-boot-jar-classes> tag present, simply add\n  the new includes to the existing one\n");
        sb.append("\n");
        sb.append("It's possible that this class is truly not-portable, the solution is then to\nmark the referring field as transient.\n");
        break;
      case TEST_REASON:
        sb.append("instructions");
        break;
    }
    
    return sb.toString();
  }

  public boolean hasUltimateNonPortableFieldName() {
    return this.ultimateNonPortableFieldName != null;
  }

  public void setUltimateNonPortableFieldName(String name) {
    addDetail("Referring field", name);
    this.ultimateNonPortableFieldName = name;
  }

  public String getUltimateNonPortableFieldName() {
    return this.ultimateNonPortableFieldName;
  }

  private String getErroneousSuperClassNames() {
    Collection supers = new ArrayList(nonBootJarClasses);
    supers.addAll(bootJarClasses);
    return csvList(supers);
  }

  private String csvList(Collection list) {
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

  public byte getReason() {
    return reason;
  }

  public void addErroneousSuperClass(Class superClass) {
    if (superClass.getClassLoader() == null) {
      bootJarClasses.add(superClass.getName());
    } else {
      nonBootJarClasses.add(superClass.getName());
    }
  }

  public List getErroneousSuperClasses() {
    return nonBootJarClasses;
  }

  public List getErroneousBootJarSuperClasses() {
    return bootJarClasses;
  }

  public String toString() {
    return getDetailedReason();
  }

  public void setMessage(String msg) {
    this.message = msg;
  }

  public String getMessage() {
    return message;
  }

  public void accept(NonPortableReasonFormatter formatter) {
    formatter.formatReasonText(getDetailedReason());
    //formatter.formatReasonText("Actions to take:");
    for (Iterator i = details.iterator(); i.hasNext();) {
      formatter.formatDetail((NonPortableDetail) i.next());
    }
    formatter.formatInstructionsText(getInstructions());
  }

}
