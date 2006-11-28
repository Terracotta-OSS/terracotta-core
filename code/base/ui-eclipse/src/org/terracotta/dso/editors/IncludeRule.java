/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors;

import com.terracottatech.configV2.Include;
import com.terracottatech.configV2.OnLoad;

public class IncludeRule extends Rule {
  public IncludeRule(Include include) {
    super(include);
  }
  
  public Include getInclude() {
    return (Include)getXmlObject();
  }

  public String getExpression() {
    return getInclude().getClassExpression();
  }
  
  public void setExpression(String expr) {
    getInclude().setClassExpression(expr);
  }

  public boolean hasHonorTransient() {
    return getInclude().getHonorTransient();
  }
  
  public void setHonorTransient(boolean honor) {
    getInclude().setHonorTransient(honor);
  }
  
  public OnLoad getOnLoad() {
    return getInclude().getOnLoad();
  }
  
  public void setDetails(RuleDetail details) {/**/}
  
  public RuleDetail getDetails() {
    return null;
  }
}
