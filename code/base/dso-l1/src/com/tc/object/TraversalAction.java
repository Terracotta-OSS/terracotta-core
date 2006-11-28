/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import java.util.List;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public interface TraversalAction {

  public void visit(List objects);
}