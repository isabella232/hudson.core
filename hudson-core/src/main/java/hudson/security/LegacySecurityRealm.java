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
 *  Kohsuke Kawaguchi, Seiji Sogabe, Winston Prakash
 *
 *******************************************************************************/ 

package hudson.security;

import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.AuthenticationException;
import org.kohsuke.stapler.StaplerRequest;
import hudson.model.Descriptor;
import hudson.Extension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;

/**
 * {@link SecurityRealm} that accepts {@link ContainerAuthentication} object
 * without any check (that is, by assuming that the such token is already
 * authenticated by the container.)
 *
 * @author Kohsuke Kawaguchi
 */
public final class LegacySecurityRealm extends SecurityRealm implements AuthenticationManager {

    public SecurityComponents createSecurityComponents() {
        return new SecurityComponents(this);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof ContainerAuthentication) {
            return authentication;
        } else {
            return null;
        }
    }

    /**
     * To have the username/password authenticated by the container, submit the
     * form to the URL defined by the servlet spec.
     */
    @Override
    public String getAuthenticationGatewayUrl() {
        return "j_security_check";
    }

    @Override
    public String getLoginUrl() {
        return "loginEntry";
    }

    /**
     * Filter to run for the LegacySecurityRealm is the ChainServletFilter
     */
    @Override
    public Filter createFilter(FilterConfig filterConfig) {

        // this filter set up is used to emulate the legacy Hudson behavior
        // of container authentication before 1.160 

        // when using container-authentication we can't hit /login directly.
        // we first have to hit protected /loginEntry, then let the container
        // trap that into /login.

        List<Filter> filters = new ArrayList<Filter>();
        BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter();
        filters.add(basicAuthenticationFilter);

        filters.addAll(Arrays.asList(getCommonFilters()));

        return new ChainedServletFilter(filters);

    }
    @Extension
    public static final Descriptor<SecurityRealm> DESCRIPTOR = new Descriptor<SecurityRealm>() {
        public SecurityRealm newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new LegacySecurityRealm();
        }

        public String getDisplayName() {
            return Messages.LegacySecurityRealm_Displayname();
        }

        public String getHelpFile() {
            return "/help/security/container-realm.html";
        }
    };
}
