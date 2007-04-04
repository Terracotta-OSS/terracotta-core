package demo.continuations;

import com.uwyn.rife.continuations.ContinuationContext;
import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.Param;
import com.uwyn.rife.engine.annotations.Submission;
import com.uwyn.rife.engine.annotations.SubmissionBean;
import com.uwyn.rife.site.ValidatedConstrained;
import com.uwyn.rife.template.Template;

/**
 *  This element handles a basic multi-step order checkout process. <p>
 *
 *  Continuations are used to handle the intermediate data submissions. By
 *  providing a custom <code>clone()</code> implementation, the individual
 *  steps are prefilled with earlier submitted data if the user has pressed
 *  the back button.
 *
 *@author     Geert Bevin (gbevin[remove] at uwyn dot com)
 *@version    $Revision$
 */
@Elem(
         submissions={
         @Submission(
            name="selectShipping",
            beans={@SubmissionBean(beanclass=OrderData.class, group=OrderDataMetaData.GROUP_SHIPPING) }) ,
         @Submission(
      name="provideCreditCard",
      beans={@SubmissionBean(beanclass=OrderData.class, group=OrderDataMetaData.GROUP_CREDITCARD) },
      params={@Param(name=Order.PARAM_BACK) })
         })
public class Order extends Element {

   private OrderData order = new OrderData();
   /**
    *  Description of the Field
    */
   public static final String PARAM_BACK = "back";

   public void processElement() {
      Template template = getHtmlTemplate("order");

      // handle the submission of the shipping details
      do {
         generateForm(template, order);
         template.setBlock("content_form", "content_shipping");
         print(template);
         pause();

         template.clear();
         ((ValidatedConstrained) order).resetValidation();
         fillSubmissionBean(order);
      } while (duringStepBack() || !((ValidatedConstrained) order).validateGroup(OrderDataMetaData.GROUP_SHIPPING));

      // handle the submission of the credit card details
      do {
         generateForm(template, order);
         template.setBlock("content_form", "content_creditcard");
         print(template);
         pause();

         template.clear();
         ((ValidatedConstrained) order).resetValidation();
         fillSubmissionBean(order);
         if (hasParameterValue(PARAM_BACK)) {
            stepBack();
         }
      } while (!((ValidatedConstrained) order).validateGroup(OrderDataMetaData.GROUP_CREDITCARD));

      // provide an overview of everything that has been submitted
      template.setBean(order);
      template.setBlock("content", "content_overview");
      print(template);

      // remove any continuation contexts that are active in this tree
      ContinuationContext.getActiveContext().removeContextTree();
   }

   public Object clone()
          throws CloneNotSupportedException {
      // This clone implementation uses the standard Element clone method.
      // The order member variable will be preserved as-is however.
      // The result is that even when the user presses the back button and
      // re-submits a previous step, the earlier submitted data will be
      // used to automatically fill in the already answered steps.
      Order cloned = (Order) super.clone();
      cloned.order = order;

      return cloned;
   }
}
