/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * AddToListForm represents the form data submitted from the display page.  
 * The ActionServlet populates this form when a request for add is received 
 * from the display page.
 */
public class AddToListForm extends ActionForm {
  private String newListItem;
  private String errorMsg;
  
  public AddToListForm() {
    super();
    resetFields();
  }

  public ActionErrors validate(ActionMapping mapping, HttpServletRequest req ){
    ActionErrors errors = new ActionErrors();
    return errors;
  }

  public void reset(ActionMapping mapping, HttpServletRequest request) {
    resetFields();
  }

  protected void resetFields() {
    newListItem = "";
    errorMsg = null;
  }

  public void setNewListItem(String nli) {
    newListItem = nli;
    errorMsg = null;
    
    if (newListItem == null ||
  (newListItem = newListItem.trim()) == null ||
   newListItem.equals("")) {
      newListItem = null;
      errorMsg = "Error: A new list item is required for \"Add\" operation";
    }
  }

  public String getNewListItem() {
    return newListItem;
  }
  
  public String getErrorMsg(){
    return errorMsg;
  }
}

