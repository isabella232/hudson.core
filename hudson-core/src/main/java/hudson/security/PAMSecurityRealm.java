/*******************************************************************************
 *
 * Copyright (c) 2004-2012, Oracle Corporation
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
import hudson.Util;
import hudson.Extension;
import hudson.util.FormValidation;
import java.util.ArrayList;
import org.eclipse.hudson.jna.NativeAccessException;
import org.eclipse.hudson.jna.NativeUtils;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link SecurityRealm} that uses Unix PAM authentication.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.282
 */
public class PAMSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    public final String serviceName;

    @DataBoundConstructor
    public PAMSecurityRealm(String serviceName) {
        serviceName = Util.fixEmptyAndTrim(serviceName);
        if (serviceName == null) {
            serviceName = "sshd"; // use sshd as the default
        }
        this.serviceName = serviceName;
    }

    @Override
    protected UserDetails authenticate(String username, String password) throws AuthenticationException {
        try {

            Set<String> grps = NativeUtils.getInstance().pamAuthenticate(serviceName, username, password);
            List<GrantedAuthority> groups = new ArrayList<GrantedAuthority>();
            for (String g : grps) {
                groups.add(new GrantedAuthorityImpl(g));
            }
            groups.add(AUTHENTICATED_AUTHORITY);
            EnvVars.setHudsonUserEnvVar(username);
            return new User(username, "", true, true, true, true, groups);
        } catch (NativeAccessException exc) {
            throw new BadCredentialsException(exc.getMessage(), exc);
        }
    }

    @Override
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
                Arrays.asList(new GrantedAuthority[]{AUTHENTICATED_AUTHORITY}));
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

        @Override
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
