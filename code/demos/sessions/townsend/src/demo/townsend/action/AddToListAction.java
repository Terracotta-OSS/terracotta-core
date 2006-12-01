/*
@COPYRIGHT@
*/
package demo.townsend.action;

import demo.townsend.common.Constants;
import demo.townsend.form.AddToListForm;
import demo.townsend.service.DataKeeper;
import demo.townsend.service.Product;
import demo.townsend.service.ProductCatalog;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * AddToListAction processes the request to add an item to the user's list.  
 * User's list is fetched from the HttpSession object, the item indicated in 
 * the AddToListForm is added to the list, and the modified list is loaded back 
 * into the HttpSession object.
 */
public class AddToListAction extends Action {
   public ActionForward execute( ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {

      String newProdId = ((AddToListForm)form).getId();
      Product newProd = null;
      ArrayList catalog = new ProductCatalog().getCatalog();
      for (Iterator iter = catalog.iterator(); iter.hasNext(); ) {
        Product p = (Product) iter.next();
        if (p.getId().equals(newProdId)) {
          newProd = p;
        }
      }
      
      HttpSession session = (HttpSession)request.getSession();
      
      DataKeeper dkeeper = (DataKeeper)session.getAttribute( Constants.DATA_KEY);
      if (dkeeper == null) {
        dkeeper = new DataKeeper();
      }
      
      dkeeper.addListItem(newProd);
      
      session.setAttribute( Constants.DATA_KEY, dkeeper );
       	  
      return mapping.findForward(Constants.SUCCESS_KEY ); 
  }
}
