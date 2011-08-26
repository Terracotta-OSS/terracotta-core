/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class QuickAssistProcessor implements org.eclipse.jdt.ui.text.java.IQuickAssistProcessor {
  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
    IJavaCompletionProposal[] result = null;
    
    if(hasAssists(context)) {
      result = new IJavaCompletionProposal[] {new DeclaredRootProposal(context)};
    }
    
    return result;
  }

  private ASTNode getAncestor(ASTNode node, int type) {
    ASTNode parent;
    
    while((parent = node.getParent()) != null) {
      if(parent.getNodeType() == type) {
        return parent;
      }
      node = parent;
    }
    
    return null;
  }
  
  public boolean hasAssists(IInvocationContext context) {
    ASTNode covered  = context.getCoveredNode();
    ASTNode covering = context.getCoveringNode();
    
    if(covered == null) covered = covering;
    
    if(covered != null && covered.getNodeType() == ASTNode.SIMPLE_NAME) {
      return getAncestor(covered, ASTNode.FIELD_DECLARATION) != null;
    }
    
    return false;
  }
}

class DeclaredRootProposal implements IJavaCompletionProposal {
  IInvocationContext m_context;
  
  DeclaredRootProposal(IInvocationContext context) {
    m_context = context;
  }
  
  public int getRelevance() {
    return 100;
  }

  public void apply(IDocument document) {
    System.out.println(document);
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return "Make a root";
  }

  public Image getImage() {
    return null;
  }

  public Point getSelection(IDocument document) {
    return null;
  }
}