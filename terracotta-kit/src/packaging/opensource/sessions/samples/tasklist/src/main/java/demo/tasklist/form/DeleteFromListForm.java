/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.form;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * DeleteFromListForm represents the form data submitted from the display page.  
 * The ActionServlet populates this form when a request for deletion is received 
 * from the display page.
 */
public class DeleteFromListForm extends ActionForm {
    private ArrayList itemsForDelete = new ArrayList();
    private String    errorMsg;
  
  public DeleteFromListForm() {
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
    errorMsg = "Error: At least one item for deletion must be selected for \"Delete\" operation";
    itemsForDelete = new ArrayList();
  }

  public String[] getItemsForDelete() { 
    return (String[])this.itemsForDelete.toArray(new String[0]); 
  } 
  
  public void setItemsForDelete(String[] itemsForDelete) {
    if (itemsForDelete == null || itemsForDelete.length == 0) {
      itemsForDelete = null;
    } else {
      errorMsg = null;
      for (int i = 0; i < itemsForDelete.length; i++) {
        this.itemsForDelete.add(itemsForDelete[i]);
      }
    }  
  }  
  
  public String getErrorMsg(){
    return errorMsg;
  }
}

