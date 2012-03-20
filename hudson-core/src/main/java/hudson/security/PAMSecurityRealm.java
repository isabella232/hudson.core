/*******************************************************************************
 *
 * Copyright (c) 2004-2009, Oracle Corporation
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

import hudson.EnvVars;
import hudson.Functions;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.Util;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.jna.NativeAccessException;
import hudson.util.jna.NativeUtils;
import java.util.Arrays;

import org.eclipse.hudson.stapler.DataBoundConstructor;
import java.util.Set;

import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.User;
import org.springframework.dao.DataAccessException;
import org.springframework.security.providers.ProviderManager;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationProvider;
import org.springframework.security.providers.rememberme.RememberMeAuthenticationProvider;

/**
 * {@link SecurityRealm} that uses Unix PAM authentication.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.282
 */
public class PAMSecurityRealm extends SecurityRealm {

    public final String serviceName;

    @DataBoundConstructor
    public PAMSecurityRealm(String serviceName) {
        serviceName = Util.fixEmptyAndTrim(serviceName);
        if (serviceName == null) {
            serviceName = "sshd"; // use sshd as the default
        }
        this.serviceName = serviceName;
    }

    public static class PAMAuthenticationProvider implements AuthenticationProvider {

        private String serviceName;

        public PAMAuthenticationProvider(String serviceName) {
            this.serviceName = serviceName;
        }

        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String username = authentication.getPrincipal().toString();
            String password = authentication.getCredentials().toString();

            try {

                Set<String> grps = NativeUtils.getInstance().pamAuthenticate(serviceName, username, password);
                GrantedAuthority[] groups = new GrantedAuthority[grps.size()];
                int i = 0;
                for (String g : grps) {
                    groups[i++] = new GrantedAuthorityImpl(g);
                }
                EnvVars.setHudsonUserEnvVar(username);
                // I never understood why Spring Security insists on keeping the password...
                return new UsernamePasswordAuthenticationToken(username, password, groups);
            } catch (NativeAccessException exc) {
                throw new BadCredentialsException(exc.getMessage(), exc);
            }

        }

        public boolean supports(Class clazz) {
            return true;
        }
    }

    public SecurityComponents createSecurityComponents() {

        // talk to PAM
        PAMAuthenticationProvider PamAuthenticationProvider = new PAMAuthenticationProvider(serviceName);

        // these providers apply everywhere
        RememberMeAuthenticationProvider rememberMeAuthenticationProvider = new RememberMeAuthenticationProvider();
        rememberMeAuthenticationProvider.setKey(Hudson.getInstance().getSecretKey());

        // this doesn't mean we allow anonymous access.
        // we just authenticate anonymous users as such,
        // so that later authorization can reject them if so configured
        AnonymousAuthenticationProvider anonymousAuthenticationProvider = new AnonymousAuthenticationProvider();
        anonymousAuthenticationProvider.setKey("anonymous");

        AuthenticationProvider[] authenticationProvider = {
            PamAuthenticationProvider,
            rememberMeAuthenticationProvider,
            anonymousAuthenticationProvider
        };

        ProviderManager providerManager = new ProviderManager();
        providerManager.setProviders(Arrays.asList(authenticationProvider));

        UserDetailsService userDetailsService = new UserDetailsService() {

            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
                try {
                    if (!NativeUtils.getInstance().checkUnixUser(username)) {
                        throw new UsernameNotFoundException("No such Unix user: " + username);
                    }
                } catch (NativeAccessException exc) {
                    throw new DataAccessException("Failed to find Unix User", exc) {
                    };
                }

                // return some dummy instance
                return new User(username, "", true, true, true, true,
                        new GrantedAuthority[]{AUTHENTICATED_AUTHORITY});
            }
        };

        return new SecurityComponents(providerManager, userDetailsService);

    }

    @Override
    public GroupDetails loadGroupByGroupname(final String groupname) throws UsernameNotFoundException, DataAccessException {
        try {
            if (!NativeUtils.getInstance().checkUnixGroup(groupname)) {
                throw new UsernameNotFoundException("No such Unix group: " + groupname);
            }
        } catch (NativeAccessException exc) {
            throw new DataAccessException("Failed to find Unix Group", exc) {
            };
        }

        return new GroupDetails() {

            @Override
            public String getName() {
                return groupname;
            }
        };
    }

    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        public String getDisplayName() {
            return Messages.PAMSecurityRealm_DisplayName();
        }

        public FormValidation doTest() {
            try {
                String message = NativeUtils.getInstance().checkPamAuthentication();
                if (message.startsWith("Error:")) {
                    return FormValidation.error(message.replaceFirst("Error:", ""));
                } else {
                    return FormValidation.ok(message);
                }
            } catch (NativeAccessException exc) {
                return FormValidation.error("Native Support for PAM Authentication not available.");
            }
        }
    }

    @Extension
    public static DescriptorImpl install() {
        if (!Functions.isWindows()) {
            return new DescriptorImpl();
        }
        return null;
    }
}
