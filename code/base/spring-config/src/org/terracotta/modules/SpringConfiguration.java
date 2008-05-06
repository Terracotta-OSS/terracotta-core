/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.TransparencyClassSpec;

public class SpringConfiguration
      extends TerracottaConfiguratorModule {

   protected void addInstrumentation(final BundleContext context) {
      super.addInstrumentation(context);
      configSpringTypes();
      configSpringWebFlowTypes();
   }
   
   private void configSpringTypes() {
      configHelper.addIncludePattern("org.springframework.context.ApplicationEvent", false, false, false);
      configHelper.addIncludePattern("com.tcspring.ApplicationContextEventProtocol", true, true, true);

      configHelper.addIncludePattern("com.tcspring.ComplexBeanId", true, true, true);
      // addIncludePattern("com.tcspring.BeanContainer", true, true, true);
      configHelper.getOrCreateSpec("com.tcspring.BeanContainer").addTransient("isInitialized"); // .setHonorTransient(true);

      // scoped beans
      // addTransient("org.springframework.web.context.request.ServletRequestAttributes$DestructionCallbackBindingListener",
      // "aw$MIXIN_0");
      configHelper.addIncludePattern("com.tcspring.SessionProtocol$DestructionCallbackBindingListener", true, true, true);
      configHelper.addIncludePattern("com.tcspring.ScopedBeanDestructionCallBack", true, true, true);

      // Spring AOP introduction/mixin classes
      configHelper.addIncludePattern("org.springframework.aop.support.IntroductionInfoSupport", true, true, true);
      configHelper.addIncludePattern("org.springframework.aop.support.DelegatingIntroductionInterceptor", true, true, true);
      configHelper.addIncludePattern("org.springframework.aop.support.DefaultIntroductionAdvisor", true, true, true);
      configHelper.addIncludePattern("gnu.trove..*", false, false, true);
      configHelper.addIncludePattern("java.lang.reflect.Proxy", false, false, false);
      configHelper.addIncludePattern("com.tc.aspectwerkz.proxy..*", false, false, true);

      // TODO remove if we find a better way using ProxyApplicator etc.
      configHelper.addIncludePattern("$Proxy..*", false, false, true);

      // backport concurrent classes
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractCollection", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractQueue", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue$Node", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.FutureTask", false, false, false);

      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.PriorityBlockingQueue", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue", false, false, false);
      configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList", false, false, false);

      final LockDefinition ld = configHelper.createLockDefinition("addApplicationListener", ConfigLockLevel.WRITE); 
      ld.commit();
      configHelper.addLock("* org.springframework.context.event.AbstractApplicationEventMulticaster.addApplicationListener(..)", ld);

      // used by WebFlow
      configHelper.addIncludePattern("org.springframework.core.enums.*", false, false, false);
      configHelper.addIncludePattern("org.springframework.binding..*", true, false, false);
      configHelper.addIncludePattern("org.springframework.validation..*", true, false, false);
      
      // used by Spring AOP/AspectJ
      TransparencyClassSpec spec = configHelper.getOrCreateSpec("org.springframework.aop.support.AopUtils");
      spec.setCustomClassAdapter(new AopUtilsClassAdapter());
   }
   
   private void configSpringWebFlowTypes() {
      configHelper.addAspectModule("org.springframework.webflow", "com.tc.object.config.SpringWebFlowAspectModule");
      configHelper.addIncludePattern("com.tcspring.DSOConversationLock", false, false, false);

      configHelper.addIncludePattern("org.springframework.webflow..*", true, false, false);

      configHelper.addIncludePattern("org.springframework.webflow.conversation.impl.ConversationEntry", false, false, false);
      configHelper.addIncludePattern("org.springframework.webflow.core.collection.LocalAttributeMap", false, false, false);
      configHelper.addIncludePattern("org.springframework.webflow.conversation.impl.*", false, false, false);

      // getOrCreateSpec("org.springframework.webflow.engine.impl.FlowSessionImpl").setHonorTransient(false).addTransient("flow");
      // flow : Flow
      // flowId : String
      // state : State
      // stateId : String
      // .addTransient("parent") // : FlowSessionImpl
      // .addTransient("scope") // : LocalAttributeMap
      // .addTransient("status"); // : FlowSessionStatus

      // all "transient" for all subclasses except "State.id"
      // getOrCreateSpec("org.springframework.webflow.engine.State") //
      // .addTransient("logger").addTransient("flow") //
      // .addTransient("entryActionList") //
      // .addTransient("exceptionHandlerSet"); //
      // getOrCreateSpec("org.springframework.webflow.engine.EndState") //
      // .addTransient("viewSelector") //
      // .addTransient("outputMapper"); //
      // getOrCreateSpec("org.springframework.webflow.engine.TransitionableState") // abstract
      // .addTransient("transitions")
      // .addTransient("exitActionList");
      // getOrCreateSpec("org.springframework.webflow.engine.ActionState") //
      // .addTransient("actionList");
      // getOrCreateSpec("org.springframework.webflow.engine.SubflowState") //
      // .addTransient("subflow") //
      // .addTransient("attributeMapper"); //
      // getOrCreateSpec("org.springframework.webflow.engine.ViewState") //
      // .addTransient("viewSelector") //
      // .addTransient("renderActionList");
      // // getOrCreateSpec("org.springframework.webflow.engine.DecisionState"); no fields

      // TODO investigate if better granularity of above classes is required
      // org.springframework.webflow.execution.repository.support.DefaultFlowExecutionRepository
      // org.springframework.webflow.execution.repository.support.AbstractConversationFlowExecutionRepository
      // org.springframework.webflow.execution.repository.support.AbstractFlowExecutionRepository
      // org.springframework.webflow.execution.repository.support.DefaultFlowExecutionRepositoryFactory
      // org.springframework.webflow.execution.repository.support.DelegatingFlowExecutionRepositoryFactory
      // org.springframework.webflow.execution.repository.support.FlowExecutionRepositoryServices
      // org.springframework.webflow.execution.repository.support.SharedMapFlowExecutionRepositoryFactory
      // org.springframework.webflow.execution.repository.conversation.impl.LocalConversationService
      // org.springframework.webflow.util.RandomGuidUidGenerator
      // org.springframework.webflow.registry.FlowRegistryImpl
      // etc...
    }

}
