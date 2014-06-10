/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import com.tc.net.core.security.TCPrincipal;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.security.PwProviderUtil;
import com.terracotta.management.user.UserRole;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A realm that authenticates against the TC server's security manager.
 *
 * @author Ludovic Orban
 */
public class TCServerRealm extends AuthorizingRealm {

  private static final String TERRACOTTA_PERM = "api:read";

  private static final Collection<String> ADMIN_PERMS = Arrays.asList("api:update", "api:create", "api:delete");

  public TCServerRealm() {
    setAuthenticationTokenClass(UsernamePasswordToken.class);
    setCachingEnabled(false);
    setAuthenticationCachingEnabled(false);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    if (token == null) {
      return null;
    }

    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken)token;

    String username = usernamePasswordToken.getUsername();
    char[] password = usernamePasswordToken.getPassword();

    TCSecurityManager tcSecurityManager = (TCSecurityManager)PwProviderUtil.getProvider();
    TCPrincipal principal = (TCPrincipal)tcSecurityManager.authenticate(username, password);
    if (principal == null) {
      return null;
    }

    List<Object> principals = Arrays.asList(principal, extractRoles(principal));
    PrincipalCollection principalCollection = new SimplePrincipalCollection(principals, getName());
    return new SimpleAuthenticationInfo(principalCollection, usernamePasswordToken.getCredentials());
  }

  // the TCPrincipal does have a set of UserRole objects, but those objects' class is in another classloader
  // so me must convert those 'foreign' UserRoles into 'local' ones.
  private Set<UserRole> extractRoles(TCPrincipal principal) {
    Set<UserRole> result = new HashSet<UserRole>();

    Set<?> roles = principal.getRoles();
    for (Object role : roles) {
      UserRole userRole = UserRole.byName(role.toString());
      result.add(userRole);
    }

    return result;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    if (!principals.getRealmNames().contains(getName())) {
      return null;
    }

    SimplePrincipalCollection principalCollection = (SimplePrincipalCollection) principals;
    List<Object> listPrincipals = principalCollection.asList();
    Set<UserRole> roles = (Set<UserRole>) listPrincipals.get(1);

    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo();

    for (UserRole r : roles) {
      sai.addRole(r.toString());
      if (r == UserRole.ADMIN) {
        sai.addStringPermissions(ADMIN_PERMS);
      } else if (r == UserRole.TERRACOTTA) {
        sai.addStringPermission(TERRACOTTA_PERM);
      }
    }

    return sai;
  }

}
