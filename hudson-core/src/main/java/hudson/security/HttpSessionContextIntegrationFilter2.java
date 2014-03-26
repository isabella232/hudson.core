/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
*    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.security;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Erases the {@link SecurityContext} persisted in {@link HttpSession} if
 * {@link InvalidatableUserDetails#isInvalid()} returns true.
 *
 * @see InvalidatableUserDetails
 */
public class HttpSessionContextIntegrationFilter2 extends SecurityContextPersistenceFilter {
    
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository(){
        @Override
        protected SecurityContext generateNewContext(){
            return new NotSerilizableSecurityContext();
        }
    };

    public HttpSessionContextIntegrationFilter2() throws ServletException {
        //setContextClass(NotSerilizableSecurityContext.class);
        super.setSecurityContextRepository(securityContextRepository);
    }

    public void doFilterHttp(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) req).getSession(false);
        if (session != null) {
            SecurityContext o = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (o != null) {
                Authentication a = o.getAuthentication();
                if (a != null) {
                    if (a.getPrincipal() instanceof InvalidatableUserDetails) {
                        InvalidatableUserDetails ud = (InvalidatableUserDetails) a.getPrincipal();
                        if (ud.isInvalid()) // don't let Spring Security see invalid security context
                        {
                            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, null);
                        }
                    }
                }
            }
        }

        super.doFilter(req, res, chain);
    }
    
}
