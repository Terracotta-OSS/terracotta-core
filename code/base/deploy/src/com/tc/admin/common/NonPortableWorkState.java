/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.NonPortableObjectState;

public class NonPortableWorkState {
  private NonPortableObjectState        fObjectState;
  private NonPortableResolutionAction[] fResolutionActions;

  public NonPortableWorkState(NonPortableObjectState objectState) {
    fObjectState = objectState;
  }

  public void setActions(NonPortableResolutionAction[] actions) {
    fResolutionActions = actions;
  }

  public NonPortableResolutionAction[] getActions() {
    return fResolutionActions;
  }

  public boolean hasSelectedActions() {
    if (fResolutionActions != null && fResolutionActions.length > 0) {
      for (int i = 0; i < fResolutionActions.length; i++) {
        if (fResolutionActions[i].isSelected()) { return true; }
      }
    }

    return false;
  }

  public boolean isPortable() {
    return fObjectState.isPortable();
  }

  public boolean isNeverPortable() {
    return fObjectState.isNeverPortable();
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

  public String[] getRequiredBootTypes() {
    return fObjectState.getRequiredBootTypes();
  }

  public String summary() {
    return fObjectState.summary();
  }

  public String shortSummary() {
    StringBuffer sb = new StringBuffer();
    sb.append(getTypeName());
    if (isNeverPortable()) {
      sb.append(" Never portable");
    } else {
      sb.append(" not portable");
    }
    return sb.toString();
  }

  public String getLabel() {
    return fObjectState.getLabel();
  }

  private static final String PRE_INSTRUMENTED_PREAMBLE                  = NonPortableMessages
                                                                             .getString("PRE_INSTRUMENTED_PREAMBLE");                 //$NON-NLS-1$
  private static final String NEVER_PORTABLE_PREAMBLE                    = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_PREAMBLE");                   //$NON-NLS-1$
  private static final String NEVER_PORTABLE_FIELD_MSG                   = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_FIELD_MSG");                  //$NON-NLS-1$
  private static final String NEVER_PORTABLE_LOGICAL_CHILD_MSG           = NonPortableMessages
                                                                             .getString("NEVER_PORTABLE_LOGICAL_CHILD_MSG");          //$NON-NLS-1$
  private static final String CONSIDER_TRANSIENT_ANCESTOR                = NonPortableMessages
                                                                             .getString("CONSIDER_TRANSIENT_ANCESTOR_MSG");           //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE          = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE");         //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG         = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG");        //$NON-NLS-1$
  private static final String NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG"); //$NON-NLS-1$
  private static final String NOT_PORTABLE_PREAMBLE                      = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_PREAMBLE");                     //$NON-NLS-1$
  private static final String NOT_PORTABLE_FIELD_MSG                     = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_FIELD_MSG");                    //$NON-NLS-1$
  private static final String NOT_PORTABLE_LOGICAL_CHILD_MSG             = NonPortableMessages
                                                                             .getString("NOT_PORTABLE_LOGICAL_CHILD_MSG");            //$NON-NLS-1$
  private static final String PORTABLE_PREAMBLE                          = NonPortableMessages
                                                                             .getString("PORTABLE_PREAMBLE");                         //$NON-NLS-1$
  private static final String PORTABLE_FIELD_MSG                         = NonPortableMessages
                                                                             .getString("PORTABLE_FIELD_MSG");                        //$NON-NLS-1$
  private static final String PORTABLE_LOGICAL_CHILD_MSG                 = NonPortableMessages
                                                                             .getString("PORTABLE_LOGICAL_CHILD_MSG");                //$NON-NLS-1$

  public String descriptionFor() {
    if (isTransient()) {
      return NonPortableMessages.getString("TRANSIENT_FIELD_MSG"); //$NON-NLS-1$
    } else if (isPreInstrumented()) {
      if (getFieldName() != null) {
        return PRE_INSTRUMENTED_PREAMBLE + "\n\n" + PORTABLE_FIELD_MSG; //$NON-NLS-1$
      } else {
        return PRE_INSTRUMENTED_PREAMBLE + "\n\n" + PORTABLE_LOGICAL_CHILD_MSG; //$NON-NLS-1$
      }
    } else if (isNeverPortable()) {
      if (getFieldName() != null) {
        return NEVER_PORTABLE_PREAMBLE + "\n\n" + NEVER_PORTABLE_FIELD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        return NEVER_PORTABLE_PREAMBLE
               + "\n\n" + NEVER_PORTABLE_LOGICAL_CHILD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
      }
    } else if (!isPortable()) {
      if (isSystemType()) {
        if (getFieldName() != null) {
          return NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE
                 + "\n\n" + NOT_PORTABLE_SYSTEM_TYPE_FIELD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
          return NOT_PORTABLE_SYSTEM_TYPE_PREAMBLE
                 + "\n\n" + NOT_PORTABLE_SYSTEM_TYPE_LOGICAL_CHILD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
        }
      } else {
        if (getFieldName() != null) {
          return NOT_PORTABLE_PREAMBLE + "\n\n" + NOT_PORTABLE_FIELD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
          return NOT_PORTABLE_PREAMBLE + "\n\n" + NOT_PORTABLE_LOGICAL_CHILD_MSG + "\n\n" + CONSIDER_TRANSIENT_ANCESTOR; //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    } else {
      if (getFieldName() != null) {
        return PORTABLE_PREAMBLE + "\n\n" + PORTABLE_FIELD_MSG; //$NON-NLS-1$
      } else {
        return PORTABLE_PREAMBLE + "\n\n" + PORTABLE_LOGICAL_CHILD_MSG; //$NON-NLS-1$
      }
    }
  }
}
