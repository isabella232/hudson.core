/*******************************************************************************
 *
 * Copyright (c) 2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Winston Prakash
 *
 *******************************************************************************/

package hudson.security;

import com.thoughtworks.xstream.XStream;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.markup.MarkupFormatter;
import hudson.markup.RawHtmlMarkupFormatter;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.SpringSecurityException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;

/**
 * Manager that manages Hudson Security. The configuration is written to the file hudson-security.xml
 *
 * @author Winston Prakash
 * @since 3.0.0
 */
public class HudsonSecurityManager implements Saveable {

    /**
     * Used to load/save Security configuration.
     */
    private static final XStream XSTREAM = new XStream2();
    private transient Logger logger = LoggerFactory.getLogger(HudsonSecurityManager.class);
    /**
     * {@link Authentication} object that represents the anonymous user. Because
     * Spring Security creates its own {@link AnonymousAuthenticationToken}
     * instances, the code must not expect the singleton semantics. This is just
     * a convenient instance.
     *
     * @since 1.343
     */
    public static final Authentication ANONYMOUS = new AnonymousAuthenticationToken(
            "anonymous", "anonymous", new GrantedAuthority[]{new GrantedAuthorityImpl("anonymous")});
    /**
     * Controls a part of the <a
     * href="http://en.wikipedia.org/wiki/Authentication">authentication</a>
     * handling in Hudson. <p> Intuitively, this corresponds to the user
     * database.
     *
     * See {@link HudsonFilter} for the concrete authentication protocol.
     *
     * Never null. Always use {@link #setSecurityRealm(SecurityRealm)} to update
     * this field.
     *
     * @see #getSecurity()
     * @see #setSecurityRealm(SecurityRealm)
     */
    private volatile SecurityRealm securityRealm = SecurityRealm.NO_AUTHENTICATION;
    
    public transient final ServletContext servletContext;
    
    /**
     * Controls how the <a
     * href="http://en.wikipedia.org/wiki/Authorization">authorization</a> is
     * handled in Hudson. <p> This ultimately controls who has access to what.
     *
     * Never null.
     */
    private volatile AuthorizationStrategy authorizationStrategy = AuthorizationStrategy.UNSECURED;
    
    /**
     * False to enable anyone to do anything. Left as a field so that we can
     * still read old data that uses this flag.
     *
     * @see #authorizationStrategy
     * @see #securityRealm
     */
    private Boolean useSecurity;
    
    private MarkupFormatter markupFormatter;

    static {
        XSTREAM.alias("hudsonSecurityManager", HudsonSecurityManager.class);
    }

    public HudsonSecurityManager(ServletContext context) {
        servletContext = context;
    }

    /**
     * Gets the markup formatter used in the system.
     *
     * @return never null.
     */
    public MarkupFormatter getMarkupFormatter() {
        return markupFormatter != null ? markupFormatter : RawHtmlMarkupFormatter.INSTANCE;
    }

    /**
     * Sets the markup formatter used in the system globally.
     */
    public void setMarkupFormatter(MarkupFormatter markupFormatter) {
        this.markupFormatter = markupFormatter;
    }

    /**
     * Returns the {@link ACL} for this object.
     */
    public ACL getACL() {
        return authorizationStrategy.getRootACL();
    }

    /**
     * Short for {@code getACL().checkPermission(p)}
     */
    public void checkPermission(Permission p) {
        getACL().checkPermission(p);
    }

    /**
     * Short for {@code getACL().hasPermission(p)}
     */
    public boolean hasPermission(Permission p) {
        return getACL().hasPermission(p);
    }

    /**
     * A convenience method to check if there's some security restrictions in
     * place.
     */
    public boolean isUseSecurity() {
        return securityRealm != SecurityRealm.NO_AUTHENTICATION || authorizationStrategy != AuthorizationStrategy.UNSECURED;
    }

    /**
     * Returns the constant that captures the three basic security modes in
     * Hudson.
     */
    public SecurityMode getSecurity() {
        // fix the variable so that this code works under concurrent modification to securityRealm.
        SecurityRealm realm = securityRealm;

        if (realm == SecurityRealm.NO_AUTHENTICATION) {
            return SecurityMode.UNSECURED;
        }
        if (realm instanceof LegacySecurityRealm) {
            return SecurityMode.LEGACY;
        }
        return SecurityMode.SECURED;
    }

    /**
     * Get the configured Security Realm 
     * @return never null.
     */
    public SecurityRealm getSecurityRealm() {
        return securityRealm;
    }

    /**
     * Set a Security Realm to the Manager
     * @param securityRealm 
     */
    public void setSecurityRealm(SecurityRealm securityRealm) {
        if (securityRealm == null) {
            securityRealm = SecurityRealm.NO_AUTHENTICATION;
        }
        this.securityRealm = securityRealm;
        // reset the filters and proxies for the new SecurityRealm
        try {
            HudsonFilter filter = HudsonFilter.get(servletContext);
            if (filter == null) {
                // Fix for #3069: This filter is not necessarily initialized before the servlets.
                // when HudsonFilter does come back, it'll initialize itself.
                logger.debug("HudsonFilter has not yet been initialized: Can't perform security setup for now");
            } else {
                logger.debug("HudsonFilter has been previously initialized: Setting security up");
                filter.reset(securityRealm);
                logger.debug("Security is now fully set up");
            }
        } catch (ServletException e) {
            // for binary compatibility, this method cannot throw a checked exception
            throw new SpringSecurityException("Failed to configure filter", e) {
            };
        }
    }

    /**
     * Get the configured Authorization Strategy
     * @return never null.
     */
    public AuthorizationStrategy getAuthorizationStrategy() {
        return authorizationStrategy;
    }

    /**
     * Set the Authorization Strategy to the Manager
     * @param a 
     */
    public void setAuthorizationStrategy(AuthorizationStrategy authStrategy) {
        if (authStrategy == null) {
            authStrategy = AuthorizationStrategy.UNSECURED;
        }
        authorizationStrategy = authStrategy;
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        BulkChange bc = new BulkChange(this);
        try {
            checkPermission(Hudson.ADMINISTER);

            JSONObject json = req.getSubmittedForm();

            // keep using 'useSecurity' field as the main configuration setting
            // until we get the new security implementation working
            // useSecurity = null;
            if (json.has("use_security")) {
                useSecurity = true;
                JSONObject security = json.getJSONObject("use_security");
                setSecurityRealm(SecurityRealm.all().newInstanceFromRadioList(security, "realm"));
                setAuthorizationStrategy(AuthorizationStrategy.all().newInstanceFromRadioList(security, "authorization"));

                if (security.has("markupFormatter")) {
                    markupFormatter = req.bindJSON(MarkupFormatter.class, security.getJSONObject("markupFormatter"));
                } else {
                    markupFormatter = null;
                }
            } else {
                useSecurity = null;
                setSecurityRealm(SecurityRealm.NO_AUTHENTICATION);
                authorizationStrategy = AuthorizationStrategy.UNSECURED;
                markupFormatter = null;
            }

            rsp.sendRedirect(req.getContextPath() + '/');  // go to the top page

        } finally {
            bc.commit();
        }
    }

    /**
     * Perform the logout action for the current user.
     * @param req
     * @param rsp
     * @throws IOException
     * @throws ServletException 
     */
    public void doLogout(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        securityRealm.doLogout(req, rsp);
    }
    
    /**
     * The file where the Security settings are saved.
     */
    protected final XmlFile getConfigFile() {
        return new XmlFile(XSTREAM, new File(Hudson.getInstance().getRootDir(), "/hudson-security.xml"));
    }

    /**
     * Save the settings to the configuration file.
     */
    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }
        getConfigFile().write(this);
        SaveableListener.fireOnChange(this, getConfigFile());
    }

    /**
     * Load the settings from the configuration file
     */
    public void load() {

        XmlFile config = getConfigFile();
        try {
            if (config.exists()) {
                config.unmarshal(this);
            }
        } catch (IOException e) {
            logger.error("Failed to load " + config, e);
        }

        // read in old data that doesn't have the security field set
        if (authorizationStrategy == null) {
            if (useSecurity == null || !useSecurity) {
                authorizationStrategy = AuthorizationStrategy.UNSECURED;
            } else {
                authorizationStrategy = new FullControlOnceLoggedInAuthorizationStrategy();
            }
        }
        if (securityRealm == null) {
            if (useSecurity == null || !useSecurity) {
                setSecurityRealm(SecurityRealm.NO_AUTHENTICATION);
            } else {
                setSecurityRealm(new LegacySecurityRealm());
            }
        } else {
            // force the set to proxy
            setSecurityRealm(securityRealm);
        }

        if (useSecurity != null && !useSecurity) {
            // forced reset to the unsecure mode.
            // this works as an escape hatch for people who locked themselves out.
            authorizationStrategy = AuthorizationStrategy.UNSECURED;
            setSecurityRealm(SecurityRealm.NO_AUTHENTICATION);
        }
    }

    /**
     * Convenient static method to provide full control
     */
    public static void grantFullControl() {
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
    }

}
