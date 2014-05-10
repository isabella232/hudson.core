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
 *  Kohsuke Kawaguchi, Winston Prakash, Jean-Baptiste Quenot, Tom Huybrechts
 *
 *******************************************************************************/ 

package org.eclipse.hudson;

import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import hudson.EnvVars;
import hudson.model.Hudson;
import hudson.util.*;
import org.eclipse.hudson.WebAppController.DefaultInstallStrategy;
import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.jelly.JellyFacet;
import org.apache.tools.ant.types.FileSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletResponse;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.security.Security;
import org.eclipse.hudson.graph.ChartUtil;
import org.eclipse.hudson.init.InitialSetup;
import org.eclipse.hudson.init.InitialSetupLogin;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point when Hudson is used as a webapp.
 *
 * @author Kohsuke Kawaguchi, Winston Prakash
 */
public final class HudsonServletContextListener implements ServletContextListener {

    private Logger logger = LoggerFactory.getLogger(InitialSetup.class);
    private final RingBufferLogHandler handler = new RingBufferLogHandler();

    /**
     * Creates the sole instance of {@link Hudson} and register it to the
     * {@link ServletContext}.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            final ServletContext servletContext = event.getServletContext();

            // Install the current servlet context, unless its already been set
            final WebAppController controller = WebAppController.get();
            try {
                // Attempt to set the context
                controller.setContext(servletContext);
            } catch (IllegalStateException e) {
                // context already set ignore
            }

            // Setup the default install strategy if not already configured
            try {
                controller.setInstallStrategy(new DefaultInstallStrategy());
            } catch (IllegalStateException e) {
                // strategy already set ignore
            }

            // use the current request to determine the language
            LocaleProvider.setProvider(new LocaleProvider() {
                @Override
                public Locale get() {
                    Locale locale = null;
                    StaplerRequest req = Stapler.getCurrentRequest();
                    if (req != null) {
                        locale = req.getLocale();
                    }
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                    return locale;
                }
            });

            // quick check to see if we (seem to) have enough permissions to run. (see #719)
            JVM jvm;
            try {
                jvm = new JVM();
                new URLClassLoader(new URL[0], getClass().getClassLoader());
            } catch (SecurityException e) {
                controller.install(new InsufficientPermissionDetected(e));
                return;
            }

            try {
                // remove Sun PKCS11 provider if present. See http://wiki.hudson-ci.org/display/HUDSON/Solaris+Issue+6276483
                Security.removeProvider("SunPKCS11-Solaris");
            } catch (SecurityException e) {
                // ignore this error.
            }

            installLogger();

            File dir = getHomeDir(event);
            try {
                dir = dir.getCanonicalFile();
            } catch (IOException e) {
                dir = dir.getAbsoluteFile();
            }
            final File home = dir;
            home.mkdirs();

            logger.info("Home directory: " + home);

            // check that home exists (as mkdirs could have failed silently), otherwise throw a meaningful error
            if (!home.exists()) {
                controller.install(new NoHomeDir(home));
                return;
            }


            // make sure that we are using XStream in the "enhanced" (JVM-specific) mode
            if (jvm.bestReflectionProvider().getClass() == PureJavaReflectionProvider.class) {
                // nope
                controller.install(new IncompatibleVMDetected());
                return;
            }

            // make sure this is servlet 2.4 container or above
            try {
                ServletResponse.class.getMethod("setCharacterEncoding", String.class);
            } catch (NoSuchMethodException e) {
                controller.install(new IncompatibleServletVersionDetected(ServletResponse.class));
                return;
            }

            // make sure that we see Ant 1.7
            try {
                FileSet.class.getMethod("getDirectoryScanner");
            } catch (NoSuchMethodException e) {
                controller.install(new IncompatibleAntVersionDetected(FileSet.class));
                return;
            }

            //make sure AWT is functioning. Needed for Graphing framework to work properly
            if (ChartUtil.awtProblemCause != null) {
                controller.install(new AWTProblem(ChartUtil.awtProblemCause));
                return;
            }

            // some containers (in particular Tomcat) doesn't abort a launch
            // even if the temp directory doesn't exist.
            // check that and report an error
            try {
                File f = File.createTempFile("test", "test");
                f.delete();
            } catch (IOException e) {
                controller.install(new NoTempDir(e));
                return;
            }

            // Tomcat breaks XSLT with JDK 5.0 and onward. Check if that's the case, and if so,
            // try to correct it
            try {
                TransformerFactory.newInstance();
                // if this works we are all happy
            } catch (TransformerFactoryConfigurationError x) {
                // no it didn't.
                logger.warn("XSLT not configured correctly. Hudson will try to fix this. See http://issues.apache.org/bugzilla/show_bug.cgi?id=40895 for more details", x);
                System.setProperty(TransformerFactory.class.getName(), "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
                try {
                    TransformerFactory.newInstance();
                    logger.info("XSLT is set to the JAXP RI in JRE");
                } catch (TransformerFactoryConfigurationError y) {
                    logger.error("Failed to correct the problem.");
                }
            }

            installExpressionFactory(event);

            // Do the initial setup (if needed) before actually starting Hudson
            
            boolean securityLoadFailed = false;
            try {
                // Create the Security Manager temporarily. Since the plugins are not loaded yet
                // all permissions may not be loaded. The Security manager will reload when Hudson
                // fully starts later
                HudsonSecurityEntitiesHolder.setHudsonSecurityManager(new HudsonSecurityManager(home));
            } catch (Exception ex) {
                ex.printStackTrace();
                securityLoadFailed = true;
                logger.info("Failed to load Security. " + ex.getLocalizedMessage() + ". Disablbling security and continuing.. ");
            }

            InitialSetup initSetup = new InitialSetup(home, servletContext);
            if (initSetup.needsInitSetup()) {
                logger.info("\n\n\n================>\n\nInitial setup required. Please go to the Hudson Dashboard and complete the setup.\n\n<================\n\n\n");
                if (HudsonSecurityEntitiesHolder.getHudsonSecurityManager().isUseSecurity() && !securityLoadFailed) {
                    controller.install(new InitialSetupLogin(initSetup));
                } else {
                    controller.install(initSetup);
                }
            } else {
                initSetup.invokeHudson();
            }

        } catch (Exception exc) {
            logger.error("Failed to initialize Hudson", exc);
        } catch (Error e) {
            logger.error("Failed to initialize Hudson", e);
        }
    }

    public static void installExpressionFactory(ServletContextEvent event) {
        JellyFacet.setExpressionFactory(event, new ExpressionFactory2());
    }

    /**
     * Installs log handler to monitor all Hudson logs.
     */
    private void installLogger() {
        Hudson.logRecords = handler.getView();
        java.util.logging.Logger.getLogger("hudson").addHandler(handler);
    }

    /**
     * Determines the home directory for Hudson.
     *
     * People makes configuration mistakes, so we are trying to be nice with
     * those by doing {@link String#trim()}.
     */
    private File getHomeDir(ServletContextEvent event) {
        // check JNDI for the home directory first
        try {
            InitialContext iniCtxt = new InitialContext();
            Context env = (Context) iniCtxt.lookup("java:comp/env");
            String value = (String) env.lookup("HUDSON_HOME");
            if (value != null && value.trim().length() > 0) {
                return new File(value.trim());
            }
            // look at one more place. See issue #1314 
            value = (String) iniCtxt.lookup("HUDSON_HOME");
            if (value != null && value.trim().length() > 0) {
                return new File(value.trim());
            }
        } catch (NamingException e) {
            // ignore
        }

        // finally check the system property
        String sysProp = System.getProperty("HUDSON_HOME");
        if (sysProp != null) {
            return new File(sysProp.trim());
        }

        // look at the env var next
        String env = EnvVars.masterEnvVars.get("HUDSON_HOME");
        if (env != null) {
            return new File(env.trim()).getAbsoluteFile();
        }

        // otherwise pick a place by ourselves

        String root = event.getServletContext().getRealPath("/WEB-INF/workspace");
        if (root != null) {
            File ws = new File(root.trim());
            if (ws.exists()) // Hudson <1.42 used to prefer this before ~/.hudson, so
            // check the existence and if it's there, use it.
            // otherwise if this is a new installation, prefer ~/.hudson
            {
                return ws;
            }
        }

        // if for some reason we can't put it within the webapp, use home directory.
        return new File(new File(System.getProperty("user.home")), ".hudson");
    }

    public void contextDestroyed(ServletContextEvent event) {
        Hudson instance = Hudson.getInstance();
        if (instance != null) {
            instance.cleanUp();
        }

        // Logger is in the system classloader, so if we don't do this
        // the whole web app will never be undepoyed.
        java.util.logging.Logger.getLogger("hudson").removeHandler(handler);
    }
}
