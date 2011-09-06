/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableObjectState;
import com.tc.object.appevent.NonPortableRootContext;

import java.util.List;

public class NonPortableWorkState extends AbstractWorkState {
  private NonPortableObjectState fObjectState;

  public NonPortableWorkState(NonPortableObjectState objectState) {
    fObjectState = objectState;
  }

  public boolean isPortable() {
    return fObjectState.isPortable();
  }

  public boolean isNeverPortable() {
    return fObjectState.isNeverPortable();
  }

  public boolean extendsLogicallyManagedType() {
    return fObjectState.extendsLogicallyManagedType();
  }

  public boolean isRequiredBootJarType() {
    return fObjectState.isRequiredBootJarType();
  }

  public boolean isPreInstrumented() {
    return fObjectState.isPreInstrumented();
  }

  public boolean isRepeated() {
    return fObjectState.isRepeated();
  }

  public boolean isTransient() {
    return fObjectState.isTransient();
  }

  public boolean isSystemType() {
    return fObjectState.isSystemType();
  }

  public boolean isNull() {
    return fObjectState.isNull();
  }

  public String getTypeName() {
    return fObjectState.getTypeName();
  }

  public String getFieldName() {
    return fObjectState.getFieldName();
  }

  public boolean hasRequiredBootTypes() {
    List requiredBootTypes = getRequiredBootTypes();
    return requiredBootTypes != null && requiredBootTypes.size() > 0;
  }

  public boolean thisIsOnlyRequiredBootType() {
    List requiredBootTypes = getRequiredBootTypes();
    return requiredBootTypes != null && requiredBootTypes.size() == 1 && requiredBootTypes.get(0).equals(getTypeName());
  }

  public List getRequiredBootTypes() {
    return fObjectState.getRequiredBootTypes();
  }

  public boolean hasNonPortableBaseTypes() {
    List nonPortableBaseTypes = getNonPortableBaseTypes();
    return nonPortableBaseTypes != null && nonPortableBaseTypes.size() > 0;
  }

  public List getNonPortableBaseTypes() {
    return fObjectState.getNonPortableBaseTypes();
  }

  public boolean hasRequiredIncludeTypes() {
    List requiredIncludeTypes = getRequiredIncludeTypes();
    return requiredIncludeTypes != null && requiredIncludeTypes.size() > 0;
  }

  public List getRequiredIncludeTypes() {
    return fObjectState.getRequiredIncludeTypes();
  }

  public String summary() {
    return fObjectState.summary();
  }

  public String shortSummary() {
    StringBuffer sb = new StringBuffer();
    sb.append(getTypeName());
    if (isNeverPortable()) {
      sb.append(" Never portable");
    } else if(!isPortable()){
      sb.append(" not portable");
    } else {
      sb.append(" is excluded");
    }
    return sb.toString();
  }

  public String getLabel() {
    return fObjectState.getLabel();
  }

  public String getExplaination() {
    return fObjectState.getExplaination();
  }

  private static final String TRANSIENT_FIELD_MSG                        = NonPortableMessages
                                                                             .getString("TRANSIENT_FIELD_MSG");                       //$NON-NLS-1$
  private static final String PRE_INSTRUMENTED_PREAMBLE                  = NonPortableMessages
                                                                             .getString("PRE_INSTRUMENTED_PREAMBLE");                 //$NON-NLS-1$
  private static final String NEVER_PORTABLE_PREAMBLE                    = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_PREAMBLE");                   //$NON-NLS-1$
  private static final String NEVER_PORTABLE_FIELD_MSG                   = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_FIELD_MSG");                  //$NON-NLS-1$
  private static final String NEVER_PORTABLE_LOGICAL_CHILD_MSG           = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_LOGICAL_CHILD_MSG");          //$NON-NLS-1$
  private static final String NEVER_PORTABLE_ROOT_MSG                    = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_ROOT_MSG");                   //$NON-NLS-1$
  private static final String CONSIDER_TRANSIENT_ANCESTOR                = NonPortableMessages
                                                                             .getString("CONSIDER_TRANSIENT_ANCESTOR_MSG");           //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE          = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE");         //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG         = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG");        //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG"); //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_ROOT_MSG          = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_ROOT_MSG");         //$NON-NLS-1$
  private static final String NOT_PORTABLE_PREAMBLE                      = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_PREAMBLE");                     //$NON-NLS-1$
  private static final String NOT_PORTABLE_FIELD_MSG                     = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_FIELD_MSG");                    //$NON-NLS-1$
  private static final String NOT_PORTABLE_LOGICAL_CHILD_MSG             = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_LOGICAL_CHILD_MSG");            //$NON-NLS-1$
  private static final String NOT_PORTABLE_ROOT_MSG                      = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_ROOT_MSG");                     //$NON-NLS-1$
  private static final String PORTABLE_PREAMBLE                          = NonPortableMessages
                                                                             .getString("PORTABLE_PREAMBLE");                         //$NON-NLS-1$
  private static final String PORTABLE_FIELD_MSG                         = NonPortableMessages
                                                                             .getString("PORTABLE_FIELD_MSG");                        //$NON-NLS-1$
  private static final String PORTABLE_LOGICAL_CHILD_MSG                 = NonPortableMessages
                                                                             .getString("PORTABLE_LOGICAL_CHILD_MSG");                //$NON-NLS-1$
  private static final String NON_PORTABLE_BASE_TYPE_MSG                 = NonPortableMessages
                                                                             .getString("NON_PORTABLE_BASE_TYPE_MSG");                //$NON-NLS-1$
  private static final String REQUIRED_BOOT_JAR_TYPE_MSG                 = NonPortableMessages
                                                                             .getString("REQUIRED_BOOT_JAR_TYPE_MSG");                //$NON-NLS-1$
  private static final String EXTENDS_LOGICALLY_MANAGED_TYPE_MSG         = NonPortableMessages
                                                                             .getString("EXTENDS_LOGICALLY_MANAGED_TYPE_MSG");        //$NON-NLS-1$
  private static final String WHITESPACE                                 = "\n\n";

  private static void append(StringBuffer sb, String[] strings) {
    if (strings != null) {
      for (int i = 0; i < strings.length; i++) {
        sb.append(strings[i]);
      }
    }
  }

  private boolean isRoot(final NonPortableEventContext context) {
    if (context instanceof NonPortableRootContext) { return ((NonPortableRootContext) context).getFieldName()
        .equals(getFieldName()); }
    return false;
  }

  public String descriptionFor(final ApplicationEventContext context) {
    if (!(context instanceof NonPortableEventContext)) return "";

    String explaination = getExplaination();
    if (explaination != null) { return explaination; }

    boolean isRoot = isRoot((NonPortableEventContext) context);
    StringBuffer sb = new StringBuffer();
    if (isTransient()) {
      sb.append(TRANSIENT_FIELD_MSG);
    } else if (isPreInstrumented()) {
      sb.append(PRE_INSTRUMENTED_PREAMBLE);
      if (!isRoot) {
        if (getFieldName() != null) {
          append(sb, new String[] { WHITESPACE, PORTABLE_FIELD_MSG });
        } else {
          append(sb, new String[] { WHITESPACE, PORTABLE_LOGICAL_CHILD_MSG });
        }
      }
    } else if (isNeverPortable()) {
      sb.append(NEVER_PORTABLE_PREAMBLE);
      if (!isRoot) {
        if (getFieldName() != null) {
          append(sb, new String[] { WHITESPACE, NEVER_PORTABLE_FIELD_MSG, WHITESPACE, CONSIDER_TRANSIENT_ANCESTOR });
        } else {
          append(sb, new String[] { WHITESPACE, NEVER_PORTABLE_LOGICAL_CHILD_MSG, WHITESPACE,
              CONSIDER_TRANSIENT_ANCESTOR });
        }
      } else {
        append(sb, new String[] { WHITESPACE, NEVER_PORTABLE_ROOT_MSG });
      }
    } else if (!isPortable()) {
      String reason = "";

      if (hasNonPortableBaseTypes()) reason = " " + NON_PORTABLE_BASE_TYPE_MSG;
      if (hasRequiredBootTypes() && !thisIsOnlyRequiredBootType()) {
        reason += " " + REQUIRED_BOOT_JAR_TYPE_MSG;
      }

      if (isSystemType()) {
        append(sb, new String[] { NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE, reason });
        if (!isRoot) {
          if (getFieldName() != null) {
            append(sb, new String[] { WHITESPACE, NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG, WHITESPACE,
                CONSIDER_TRANSIENT_ANCESTOR });
          } else {
            append(sb, new String[] { WHITESPACE, NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG, WHITESPACE,
                CONSIDER_TRANSIENT_ANCESTOR });
          }
        } else {
          append(sb, new String[] { WHITESPACE, NOT_PORTABLE_SYSTEM_TYPE_ROOT_MSG });
        }
      } else if (extendsLogicallyManagedType()) {
        sb.append(EXTENDS_LOGICALLY_MANAGED_TYPE_MSG);
      } else {
        append(sb, new String[] { NOT_PORTABLE_PREAMBLE, reason });
        if (!isRoot) {
          if (getFieldName() != null) {
            append(sb, new String[] { WHITESPACE, NOT_PORTABLE_FIELD_MSG, WHITESPACE, CONSIDER_TRANSIENT_ANCESTOR });
          } else {
            append(sb, new String[] { WHITESPACE, NOT_PORTABLE_LOGICAL_CHILD_MSG, WHITESPACE,
                CONSIDER_TRANSIENT_ANCESTOR });
          }
        } else {
          append(sb, new String[] { WHITESPACE, NOT_PORTABLE_ROOT_MSG });
        }
      }
    } else {
      sb.append(PORTABLE_PREAMBLE);
      if (!isRoot) {
        if (getFieldName() != null) {
          append(sb, new String[] { WHITESPACE, PORTABLE_FIELD_MSG });
        } else {
          append(sb, new String[] { WHITESPACE, PORTABLE_LOGICAL_CHILD_MSG });
        }
      }
    }

    return sb.toString();
  }
}
