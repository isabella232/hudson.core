/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Kohsuke Kawaguchi, Winston Prakash
 *
 *******************************************************************************/ 

package hudson.security;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

/**
 * {@link Authentication} implementation for {@link Principal} given through
 * {@link HttpServletRequest}.
 *
 * <p> This is used to plug the container authentication to Spring Security, for
 * backward compatibility with Hudson &lt; 1.160.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ContainerAuthentication implements Authentication {

    private final Principal principal;
    private final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();;

    /**
     * Servlet container can tie a {@link ServletRequest} to the request
     * handling thread, so we need to capture all the information upfront to
     * allow {@link Authentication} to be passed to other threads, like update
     * center does. See HUDSON-5382.
     * @param request
     */
    public ContainerAuthentication(HttpServletRequest request) {
        this.principal = request.getUserPrincipal();
        if (principal == null) {
            throw new IllegalStateException(); // for anonymous users, we just don't call SecurityContextHolder.getContext().setAuthentication.   
        }
        // Servlet API doesn't provide a way to list up all roles the current user
        // has, so we need to ask AuthorizationStrategy what roles it is going to check against.
         
        for (String g : HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getAuthorizationStrategy().getGroups()) {
            if (request.isUserInRole(g)) {
                authorities.add(new GrantedAuthorityImpl(g));
            }
        }
        authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return principal.getName();
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // noop
    }

    @Override
    public String getName() {
        return getPrincipal();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
         return authorities;
    }
}
