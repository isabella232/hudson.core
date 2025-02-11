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
 *  Kohsuke Kawaguchi, Seiji Sogabe, Winston Prakash
 *
 *******************************************************************************/ 

package hudson.security;

import hudson.Extension;
import static hudson.Util.fixNull;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixEmpty;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.tasks.MailAddressResolver;
import hudson.util.FormValidation;
import hudson.util.Scrambler;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.dao.DataAccessException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;


/**
 * {@link SecurityRealm} implementation that uses LDAP for authentication.
 *
 *
 * <h2>Key Object Classes</h2>
 *
 * <h4>Group Membership</h4>
 *
 * <p> Two object classes seem to be relevant. These are in RFC 2256 and
 * core.schema. These use DN for membership, so it can create a group of
 * anything. I don't know what the difference between these two are.
 * <pre>
 * attributetype ( 2.5.4.31 NAME 'member'
 * DESC 'RFC2256: member of a group'
 * SUP distinguishedName )
 *
 * attributetype ( 2.5.4.50 NAME 'uniqueMember'
 * DESC 'RFC2256: unique member of a group'
 * EQUALITY uniqueMemberMatch
 * SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
 *
 * objectclass ( 2.5.6.9 NAME 'groupOfNames'
 * DESC 'RFC2256: a group of names (DNs)'
 * SUP top STRUCTURAL
 * MUST ( member $ cn )
 * MAY ( businessCategory $ seeAlso $ owner $ ou $ o $ description ) )
 *
 * objectclass ( 2.5.6.17 NAME 'groupOfUniqueNames'
 * DESC 'RFC2256: a group of unique names (DN and Unique Identifier)'
 * SUP top STRUCTURAL
 * MUST ( uniqueMember $ cn )
 * MAY ( businessCategory $ seeAlso $ owner $ ou $ o $ description ) )
 * </pre>
 *
 * <p> This one is from nis.schema, and appears to model POSIX group/user thing
 * more closely.
 * <pre>
 * objectclass ( 1.3.6.1.1.1.2.2 NAME 'posixGroup'
 * DESC 'Abstraction of a group of accounts'
 * SUP top STRUCTURAL
 * MUST ( cn $ gidNumber )
 * MAY ( userPassword $ memberUid $ description ) )
 *
 * attributetype ( 1.3.6.1.1.1.1.12 NAME 'memberUid'
 * EQUALITY caseExactIA5Match
 * SUBSTR caseExactIA5SubstringsMatch
 * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
 *
 * objectclass ( 1.3.6.1.1.1.2.0 NAME 'posixAccount'
 * DESC 'Abstraction of an account with POSIX attributes'
 * SUP top AUXILIARY
 * MUST ( cn $ uid $ uidNumber $ gidNumber $ homeDirectory )
 * MAY ( userPassword $ loginShell $ gecos $ description ) )
 *
 * attributetype ( 1.3.6.1.1.1.1.0 NAME 'uidNumber'
 * DESC 'An integer uniquely identifying a user in an administrative domain'
 * EQUALITY integerMatch
 * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 SINGLE-VALUE )
 *
 * attributetype ( 1.3.6.1.1.1.1.1 NAME 'gidNumber'
 * DESC 'An integer uniquely identifying a group in an administrative domain'
 * EQUALITY integerMatch
 * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 SINGLE-VALUE )
 * </pre>
 *
 * <p> Active Directory specific schemas (from <a
 * href="http://www.grotan.com/ldap/microsoft.schema">here</a>).
 * <pre>
 * objectclass ( 1.2.840.113556.1.5.8
 * NAME 'group'
 * SUP top
 * STRUCTURAL
 * MUST (groupType )
 * MAY (member $ nTGroupMembers $ operatorCount $ adminCount $
 * groupAttributes $ groupMembershipSAM $ controlAccessRights $
 * desktopProfile $ nonSecurityMember $ managedBy $
 * primaryGroupToken $ mail ) )
 *
 * objectclass ( 1.2.840.113556.1.5.9
 * NAME 'user'
 * SUP organizationalPerson
 * STRUCTURAL
 * MAY (userCertificate $ networkAddress $ userAccountControl $
 * badPwdCount $ codePage $ homeDirectory $ homeDrive $
 * badPasswordTime $ lastLogoff $ lastLogon $ dBCSPwd $
 * localeID $ scriptPath $ logonHours $ logonWorkstation $
 * maxStorage $ userWorkstations $ unicodePwd $
 * otherLoginWorkstations $ ntPwdHistory $ pwdLastSet $
 * preferredOU $ primaryGroupID $ userParameters $
 * profilePath $ operatorCount $ adminCount $ accountExpires $
 * lmPwdHistory $ groupMembershipSAM $ logonCount $
 * controlAccessRights $ defaultClassStore $ groupsToIgnore $
 * groupPriority $ desktopProfile $ dynamicLDAPServer $
 * userPrincipalName $ lockoutTime $ userSharedFolder $
 * userSharedFolderOther $ servicePrincipalName $
 * aCSPolicyName $ terminalServer $ mSMQSignCertificates $
 * mSMQDigests $ mSMQDigestsMig $ mSMQSignCertificatesMig $
 * msNPAllowDialin $ msNPCallingStationID $
 * msNPSavedCallingStationID $ msRADIUSCallbackNumber $
 * msRADIUSFramedIPAddress $ msRADIUSFramedRoute $
 * msRADIUSServiceType $ msRASSavedCallbackNumber $
 * msRASSavedFramedIPAddress $ msRASSavedFramedRoute $
 * mS-DS-CreatorSID ) )
 * </pre>
 *
 *
 * <h2>References</h2> <dl> <dt><a
 * href="http://www.openldap.org/doc/admin22/schema.html">Standard Schemas</a>
 * <dd> The downloadable distribution contains schemas that define the structure
 * of LDAP entries. Because this is a standard, we expect most LDAP servers out
 * there to use it, although there are different objectClasses that can be used
 * for similar purposes, and apparently many deployments choose to use different
 * objectClasses.
 *
 * <dt><a href="http://www.ietf.org/rfc/rfc2256.txt">RFC 2256</a> <dd> Defines
 * the meaning of several key datatypes used in the schemas with some
 * explanations.
 *
 * <dt><a
 * href="http://msdn.microsoft.com/en-us/library/ms675085(VS.85).aspx">Active
 * Directory schema</a> <dd> More navigable schema list, including core and MS
 * extensions specific to Active Directory. </dl>
 *
 * @author Kohsuke Kawaguchi
 * @since 1.166
 */
public class LDAPSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    /**
     * LDAP server name, optionally with TCP port number, like "ldap.acme.org"
     * or "ldap.acme.org:389".
     */
    public final String server;
    /**
     * The root DN to connect to. Normally something like "dc=sun,dc=com"
     *
     * How do I infer this?
     */
    public final String rootDN;
    /**
     * Specifies the relative DN from {@link #rootDN the root DN}. This is used
     * to narrow down the search space when doing user search.
     *
     * Something like "ou=people" but can be empty.
     */
    public final String userSearchBase;
    /**
     * Query to locate an entry that identifies the user, given the user name
     * string.
     *
     * Normally "uid={0}"
     *
     * @see FilterBasedLdapUserSearch
     */
    public final String userSearch;
    /**
     * This defines the organizational unit that contains groups.
     *
     * Normally "" to indicate the full LDAP search, but can be often narrowed
     * down to something like "ou=groups"
     *
     * @see FilterBasedLdapUserSearch
     */
    public final String groupSearchBase;

    /*
     Other configurations that are needed:

     group search base DN (relative to root DN)
     group search filter (uniquemember={1} seems like a reasonable default)
     group target (CN is a reasonable default)

     manager dn/password if anonyomus search is not allowed.

     See GF configuration at http://weblogs.java.net/blog/tchangu/archive/2007/01/ldap_security_r.html
     Geronimo configuration at http://cwiki.apache.org/GMOxDOC11/ldap-realm.html
     */
    /**
     * If non-null, we use this and {@link #managerPassword} when binding to
     * LDAP.
     *
     * This is necessary when LDAP doesn't support anonymous access.
     */
    public final String managerDN;
    /**
     * Scrambled password, used to first bind to LDAP.
     */
    private final String managerPassword;
    /**
     * Created in {@link #createSecurityComponents()}. Can be used to connect to
     * LDAP.
     */
    private transient SpringSecurityLdapTemplate ldapTemplate;
    
    /**
     * LDAP filter to look for Roles of a user (Groups to which the user belongs to)
     */
    private static String ROLE_SEARCH_FILTER = System.getProperty(LDAPSecurityRealm.class.getName() + ".roleSearch", "(| (member={0}) (uniqueMember={0}) (memberUid={1}))");
    
    /**
     * LDAP filter to look for groups by their names. Can be overridden by System Property
     *
     * "{0}" is the group name as given by the user. See
     * http://msdn.microsoft.com/en-us/library/aa746475(VS.85).aspx for the
     * syntax by example. WANTED: The specification of the syntax.
     */
    public static String GROUP_SEARCH_FILTER = System.getProperty(LDAPSecurityRealm.class.getName() + ".groupSearch",
            "(& (cn={0}) (| (objectclass=groupOfNames) (objectclass=groupOfUniqueNames) (objectclass=posixGroup)))");

    @DataBoundConstructor
    public LDAPSecurityRealm(String server, String rootDN, String userSearchBase, String userSearch, String groupSearchBase, String managerDN, String managerPassword) {
        this.server = server.trim();
        this.managerDN = fixEmpty(managerDN);
        this.managerPassword = Scrambler.scramble(fixEmpty(managerPassword));
        if (fixEmptyAndTrim(rootDN) == null) {
            rootDN = fixNull(inferRootDN(server));
        }
        this.rootDN = rootDN.trim();
        this.userSearchBase = fixNull(userSearchBase).trim();
        userSearch = fixEmptyAndTrim(userSearch);
        this.userSearch = userSearch != null ? userSearch : "uid={0}";
        this.groupSearchBase = fixEmptyAndTrim(groupSearchBase);
    }

    public String getServerUrl() {
        return addPrefix(server);
    }

    /**
     * Infer the root DN.
     *
     * @return null if not found.
     */
    private String inferRootDN(String server) {
        try {
            Hashtable<String, String> props = new Hashtable<String, String>();
            if (managerDN != null) {
                props.put(Context.SECURITY_PRINCIPAL, managerDN);
                props.put(Context.SECURITY_CREDENTIALS, getManagerPassword());
            }
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            props.put(Context.PROVIDER_URL, getServerUrl() + '/');

            DirContext ctx = new InitialDirContext(props);
            Attributes atts = ctx.getAttributes("");
            Attribute a = atts.get("defaultNamingContext");
            if (a != null) // this entry is available on Active Directory. See http://msdn2.microsoft.com/en-us/library/ms684291(VS.85).aspx
            {
                return a.toString();
            }

            a = atts.get("namingcontexts");
            if (a == null) {
                LOGGER.warning("namingcontexts attribute not found in root DSE of " + server);
                return null;
            }
            return a.get().toString();
        } catch (NamingException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to LDAP to infer Root DN for " + server, e);
            return null;
        }
    }

    public String getManagerPassword() {
        return Scrambler.descramble(managerPassword);
    }

    public String getLDAPURL() {
        return getServerUrl() + '/' + fixNull(rootDN);
    }

    public synchronized SecurityComponents createSecurityComponents() {
        DefaultDirObjectFactory factory = new DefaultDirObjectFactory();
        DefaultSpringSecurityContextSource securityContextSource = new DefaultSpringSecurityContextSource(getLDAPURL());
        if (managerDN != null) {
            securityContextSource.setUserDn(managerDN);
            securityContextSource.setPassword(getManagerPassword());
        }
        Map envProps = new HashMap();
        envProps.put(Context.REFERRAL, "follow");
        securityContextSource.setDirObjectFactory(factory.getClass());
        securityContextSource.setBaseEnvironmentProperties(envProps);
        try {
            securityContextSource.afterPropertiesSet();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to set security Context for LDAP Server " + server, ex);
        }

        ldapTemplate = new SpringSecurityLdapTemplate(securityContextSource);

        FilterBasedLdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(userSearchBase, userSearch, securityContextSource);
        ldapUserSearch.setSearchSubtree(true);


        BindAuthenticator2 bindAuthenticator = new BindAuthenticator2(securityContextSource);
        bindAuthenticator.setUserSearch(ldapUserSearch);

        AuthoritiesPopulatorImpl authoritiesPopulator = new AuthoritiesPopulatorImpl(securityContextSource, groupSearchBase);
        authoritiesPopulator.setSearchSubtree(true);
        authoritiesPopulator.setGroupSearchFilter(ROLE_SEARCH_FILTER);


        // talk to LDAP
        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);

        // these providers apply everywhere
        RememberMeAuthenticationProvider rememberMeAuthenticationProvider = new RememberMeAuthenticationProvider();
        rememberMeAuthenticationProvider.setKey(HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecretKey());

        // this doesn't mean we allow anonymous access.
        // we just authenticate anonymous users as such,
        // so that later authorization can reject them if so configured
        AnonymousAuthenticationProvider anonymousAuthenticationProvider = new AnonymousAuthenticationProvider();
        anonymousAuthenticationProvider.setKey("anonymous");

        AuthenticationProvider[] authenticationProvider = {
            ldapAuthenticationProvider,
            rememberMeAuthenticationProvider,
            anonymousAuthenticationProvider
        };

        ProviderManager providerManager = new ProviderManager();
        providerManager.setProviders(Arrays.asList(authenticationProvider));
        return new SecurityComponents(providerManager, new LDAPUserDetailsService(ldapUserSearch, authoritiesPopulator));

    }

    /**
     * {@inheritDoc}
     * @param username
     * @param password
     * @return 
     */
    @Override
    protected UserDetails authenticate(String username, String password) throws AuthenticationException {
        return (UserDetails) getSecurityComponents().manager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password, ACL.NO_AUTHORITIES)).getPrincipal();
    }

    /**
     * {@inheritDoc}
     * @param username
     * @return 
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        return getSecurityComponents().userDetails.loadUserByUsername(username);
    }

    /**
     * Lookup a group; given input must match the configured syntax for group
     * names in GROUP_SEARCH_FILTER of authoritiesPopulator entry. The defaults
     * are a prefix of "ROLE_" and using all uppercase. This method will not
     * return any data if the given name lacks the proper prefix and/or case.
     * @param groupname
     * @return 
     */
    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException, DataAccessException {
        // Check proper syntax based on Spring Security configuration
        String prefix = "";
        boolean onlyUpperCase = false;
        try {
            AuthoritiesPopulatorImpl api = (AuthoritiesPopulatorImpl) ((LDAPUserDetailsService) getSecurityComponents().userDetails).authoritiesPopulator;
            prefix = api.rolePrefix;
            onlyUpperCase = api.convertToUpperCase;
        } catch (Exception ignore) {
        }
        if (onlyUpperCase && !groupname.equals(groupname.toUpperCase())) {
            throw new UsernameNotFoundException(groupname + " should be all uppercase");
        }
        if (!groupname.startsWith(prefix)) {
            throw new UsernameNotFoundException(groupname + " is missing prefix: " + prefix);
        }
        groupname = groupname.substring(prefix.length());

        // TODO: obtain a DN instead so that we can obtain multiple attributes later
        String searchBase = groupSearchBase != null ? groupSearchBase : "";
        final Set<String> groups = (Set<String>) ldapTemplate.searchForSingleAttributeValues(searchBase, GROUP_SEARCH_FILTER,
                new String[]{groupname}, "cn");

        if (groups.isEmpty()) {
            throw new UsernameNotFoundException(groupname);
        }

        return new GroupDetails() {
            public String getName() {
                return groups.iterator().next();
            }
        };
    }

    public static class LDAPUserDetailsService implements UserDetailsService {

        private final LdapUserSearch ldapSearch;
        private final LdapAuthoritiesPopulator authoritiesPopulator;

        LDAPUserDetailsService(LdapUserSearch ldapSearch, LdapAuthoritiesPopulator authoritiesPopulator) {
            this.ldapSearch = ldapSearch;
            this.authoritiesPopulator = authoritiesPopulator;
        }

        public LdapUserSearch getLdapSearch() {
            return ldapSearch;
        }

        public LdapAuthoritiesPopulator getAuthoritiesPopulator() {
            return authoritiesPopulator;
        }

        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
            LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapSearch, authoritiesPopulator);
            return ldapUserDetailsService.loadUserByUsername(username);
        }
    }

    /**
     * If the security realm is LDAP, try to pick up e-mail address from LDAP.
     */
    @Extension
    public static final class MailAdressResolverImpl extends MailAddressResolver {

        @Override
        public String findMailAddressFor(User u) {
            // LDAP not active
            SecurityRealm realm = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecurityRealm();
            if (!(realm instanceof LDAPSecurityRealm)) {
                return null;
            }
            try {
                UserDetails details =  realm.getSecurityComponents().userDetails.loadUserByUsername(u.getId());
                if (details instanceof InetOrgPerson){
                    InetOrgPerson inetOrgPerson = (InetOrgPerson) details;
                    return inetOrgPerson.getMail();
                }
                
                return null;
            } catch (UsernameNotFoundException e) {
                LOGGER.log(Level.FINE, "Failed to look up LDAP for e-mail address", e);
                return null;
            } catch (DataAccessException e) {
                LOGGER.log(Level.FINE, "Failed to look up LDAP for e-mail address", e);
                return null;
            }  
        }
    }

    /**
     * {@link LdapAuthoritiesPopulator} that adds the automatic 'authenticated'
     * role.
     */
    public static final class AuthoritiesPopulatorImpl extends DefaultLdapAuthoritiesPopulator {
        // Make these available (private in parent class and no get methods!)

        String rolePrefix;
        boolean convertToUpperCase;

        public AuthoritiesPopulatorImpl(ContextSource initialDirContextFactory, String groupSearchBase) {
            super(initialDirContextFactory, fixNull(groupSearchBase));
            // These match the defaults in Spring Security 1.0.5; set again to store in non-private fields:
            setRolePrefix("ROLE_");
            setConvertToUpperCase(true);
        }

        @Override
        protected Set getAdditionalRoles(DirContextOperations ldapUser, String username) {
            return Collections.singleton(AUTHENTICATED_AUTHORITY);
        }

        @Override
        public void setRolePrefix(String rolePrefix) {
            super.setRolePrefix(rolePrefix);
            this.rolePrefix = rolePrefix;
        }

        @Override
        public void setConvertToUpperCase(boolean convertToUpperCase) {
            super.setConvertToUpperCase(convertToUpperCase);
            this.convertToUpperCase = convertToUpperCase;
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        public String getDisplayName() {
            return Messages.LDAPSecurityRealm_DisplayName();
        }

        public FormValidation doServerCheck(
                @QueryParameter final String server,
                @QueryParameter final String managerDN,
                @QueryParameter final String managerPassword) {

            if (!HudsonSecurityEntitiesHolder.getHudsonSecurityManager().hasPermission(Hudson.ADMINISTER) || StringUtils.isEmpty(server)) {
                return FormValidation.ok();
            }

            try {
                Hashtable<String, String> props = new Hashtable<String, String>();
                if (managerDN != null && managerDN.trim().length() > 0 && !"undefined".equals(managerDN)) {
                    props.put(Context.SECURITY_PRINCIPAL, managerDN);
                }
                if (managerPassword != null && managerPassword.trim().length() > 0 && !"undefined".equals(managerPassword)) {
                    props.put(Context.SECURITY_CREDENTIALS, managerPassword);
                }
                props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                props.put(Context.PROVIDER_URL, addPrefix(server) + '/');

                DirContext ctx = new InitialDirContext(props);
                ctx.getAttributes("");
                return FormValidation.ok();   // connected
            } catch (NamingException e) {
                // trouble-shoot
                Matcher m = Pattern.compile("(ldaps://)?([^:]+)(?:\\:(\\d+))?").matcher(server.trim());
                if (!m.matches()) {
                    return FormValidation.error(Messages.LDAPSecurityRealm_SyntaxOfServerField());
                }

                try {
                    InetAddress adrs = InetAddress.getByName(m.group(2));
                    int port = m.group(1) != null ? 636 : 389;
                    if (m.group(3) != null) {
                        port = Integer.parseInt(m.group(3));
                    }
                    Socket s = new Socket(adrs, port);
                    s.close();
                } catch (UnknownHostException x) {
                    return FormValidation.error(Messages.LDAPSecurityRealm_UnknownHost(x.getMessage()));
                } catch (IOException x) {
                    return FormValidation.error(x, Messages.LDAPSecurityRealm_UnableToConnect(server, x.getMessage()));
                }

                // otherwise we don't know what caused it, so fall back to the general error report
                // getMessage() alone doesn't offer enough
                return FormValidation.error(e, Messages.LDAPSecurityRealm_UnableToConnect(server, e));
            } catch (NumberFormatException x) {
                // The getLdapCtxInstance method throws this if it fails to parse the port number
                return FormValidation.error(Messages.LDAPSecurityRealm_InvalidPortNumber());
            }
        }
    }

    /**
     * If the given "server name" is just a host name (plus optional host name),
     * add ldap:// prefix. Otherwise assume it already contains the scheme, and
     * leave it intact.
     */
    private static String addPrefix(String server) {
        if (server.contains("://")) {
            return server;
        } else {
            return "ldap://" + server;
        }
    }
    private static final Logger LOGGER = Logger.getLogger(LDAPSecurityRealm.class.getName());
}
