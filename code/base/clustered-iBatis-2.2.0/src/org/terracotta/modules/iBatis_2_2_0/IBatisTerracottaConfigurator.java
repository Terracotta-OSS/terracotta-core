/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.iBatis_2_2_0;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;
import org.terracotta.modules.iBatis_2_2_0.object.config.IBatisChangeApplicatorSpec;
import org.terracotta.modules.iBatis_2_2_0.object.config.IBatisModuleSpec;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.IStandardDSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.object.config.ModuleSpec;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;

public final class IBatisTerracottaConfigurator extends TerracottaConfiguratorModule {
  protected final void addInstrumentation(final BundleContext context, final IStandardDSOClientConfigHelper configHelper) {
    configHelper.addAutolock("* java.util.Collections$SynchronizedList.*(..)", ConfigLockLevel.WRITE);
    
    ClassAdapterFactory factory = new IBatisClassAdapter();
    
    ITransparencyClassSpec spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.mapping.result.loader.EnhancedLazyResultLoader$EnhancedLazyResultLoaderImpl");
    configHelper.addAutolock("* com.ibatis.sqlmap.engine.mapping.result.loader.EnhancedLazyResultLoader$EnhancedLazyResultLoaderImpl.loadObject(..)", ConfigLockLevel.WRITE);
    
    spec = configHelper.getOrCreateSpec(SQLException.class.getName());
    spec.markPreInstrumented();
    
    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate");
    
    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.impl.SqlMapClientImpl");
    spec.setCallConstructorOnLoad(true);
    spec.addTransient("localSqlMapSession");
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.execution.SqlExecutor", false, false, false);
    
    spec = configHelper.getOrCreateSpec("com.ibatis.common.jdbc.SimpleDataSource");
    spec.setCallConstructorOnLoad(true);
    spec.setCustomClassAdapter(factory);
    spec.addTransient("activeConnections");
    spec.addTransient("idleConnections");
    configHelper.addAutolock("* com.ibatis.common.jdbc.SimpleDataSource.*(..)", ConfigLockLevel.WRITE);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.transaction.jdbc.JdbcTransactionConfig", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.transaction.BaseTransactionConfig", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.transaction.TransactionManager", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.common.util.Throttle", false, false, false);
    configHelper.addAutolock("* com.ibatis.common.util.Throttle.*(..)", ConfigLockLevel.WRITE);
    configHelper.addIncludePattern("com.ibatis.common.util.ThrottledPool", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.type.TypeHandlerFactory", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.type.UnknownTypeHandler", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.type.BaseTypeHandler", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.*", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.statement.*", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.DomDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.ListDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.ComplexDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.PrimitiveDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.ComplexDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.BaseDataExchange", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.exchange.DataExchangeFactory", false, false, false);
    
    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.exchange.JavaBeanDataExchange");
    spec.addTransient("outParamPlan");
    spec.addTransient("parameterPlan");

    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.mapping.result.BasicResultMap");
    spec.addTransient("remappableResultMappings");
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.parameter.*", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.stat.StaticSql", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.type.*", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.result.BasicResultMapping", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.result.AutoResultMap", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.scope.*", false, false, false);
    
    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.scope.SessionScope");
    spec.addTransient("transaction");
    spec.addTransient("transactionState");
    spec.addTransient("savedTransactionState");
    
    spec = configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.scope.RequestScope");
    spec.addTransient("resultSet");
    
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.DynamicSql", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.SqlText", false, false, false);
    configHelper.addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.*", false, false, false);
    //addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.SqlTag", false, false, false);
    //addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.DynamicTagHandler", false, false, false);
    //addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.BaseTagHandler", false, false, false);
    //addIncludePattern("com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.IterateTagHandler", false, false, false);
    configHelper.addIncludePattern("com.ibatis.common.util.PaginatedArrayList", false, false, false);
    
    /*
    addIncludePattern("com.ibatis.sqlmap.engine.accessplan.EnhancedPropertyAccessPlan", false, false, false);
    spec = getOrCreateSpec("com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan");
    spec.addTransient("clazz");
    //spec.addTransient("propertyNames");
    spec.addTransient("info");
    */
    
    // IBatis DAO
    configHelper.addIncludePattern("com.ibatis.dao.engine.impl.DaoProxy", false, false, false);
    spec = configHelper.getOrCreateSpec("com.ibatis.dao.engine.impl.DaoImpl");
    
    spec = configHelper.getOrCreateSpec("com.ibatis.dao.engine.impl.StandardDaoManager");
    spec.setCallConstructorOnLoad(true);
    spec.addTransient("transactionMode");
    spec.addTransient("contextInTransactionList");
    
    spec = configHelper.getOrCreateSpec("com.ibatis.dao.engine.impl.DaoContext");
    spec.setCallConstructorOnLoad(true);
    spec.addTransient("transaction");
    spec.addTransient("state");

    configHelper.addIncludePattern("com.ibatis.dao.client.template.SqlMapDaoTemplate", false, false, false);
    configHelper.addIncludePattern("com.ibatis.dao.client.template.DaoTemplate", false, false, false);
    
    configHelper.addIncludePattern("com.ibatis.dao.engine.transaction.sqlmap.SqlMapDaoTransactionManager", false, false, false);
    
    //addIncludePattern("com.ibatis.sqlmap.engine.accessplan.PropertyAccessPlan", false, false, false);
    
    configHelper.getOrCreateSpec("com.ibatis.sqlmap.engine.impl.SqlMapClientImpl").setCustomClassAdapter(factory);
  }
  
  protected final void registerModuleSpec(final BundleContext context) {
    final Dictionary serviceProps = new Hashtable();
    serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "IBatis Plugin Spec");
    context.registerService(ModuleSpec.class.getName(), new IBatisModuleSpec(new IBatisChangeApplicatorSpec(getClass().getClassLoader())), serviceProps);
  }

}
