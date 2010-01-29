/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.townsend.form;

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
	
  //private Product product;
  private String id;
  
  public AddToListForm() {
    super();
    resetFields();
  }

  public ActionErrors validate(ActionMapping mapping, HttpServletRequest req ){
	  
	  ActionErrors errors = new ActionErrors();

    if(id == null) {
      errors.add(ActionMessages.GLOBAL_MESSAGE, 
    		  new ActionMessage("global.error.addtolist.requiredfield", "product" ));
    }
    return errors;
  }

  public void reset(ActionMapping mapping, HttpServletRequest request) {
    resetFields();
  }

  protected void resetFields() {
    id = "";
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}

