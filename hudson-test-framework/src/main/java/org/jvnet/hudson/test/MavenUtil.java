/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package org.jvnet.hudson.test;

import hudson.AbortException;
import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import hudson.model.TaskListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import java.io.PrintStream;
import java.util.StringTokenizer;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.cli.MavenLoggerManager;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Just enough of MavenUtil from legacy-maven to fix test harness bug
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=403703
 * 
 * TODO Move class back to Hudson?
 * 
 * @author Bob Foster
 */
public class MavenUtil {
  
    /**
     * Create MavenRequest given only a TaskListener. Used by HudsonTestCase.
     * 
     * @param listener
     * @return MavenRequest
     * @throws MavenEmbedderException
     * @throws IOException 
     */
    public static MavenRequest createMavenRequest(TaskListener listener) throws MavenEmbedderException, IOException {
        Properties systemProperties = new Properties();
        
        MavenRequest mavenRequest = new MavenRequest();
        
        // make sure ~/.m2 exists to avoid http://www.nabble.com/BUG-Report-tf3401736.html
        File m2Home = new File(MavenEmbedder.userHome, ".m2");
        m2Home.mkdirs();
        if(!m2Home.exists())
            throw new AbortException("Failed to create "+m2Home);

        mavenRequest.setUserSettingsFile( new File( m2Home, "settings.xml" ).getAbsolutePath() );

        mavenRequest.setGlobalSettingsFile( new File( "conf/settings.xml" ).getAbsolutePath() );
        
        mavenRequest.setUpdateSnapshots(false);

        // TODO olamy check this sould be userProperties 
        mavenRequest.setSystemProperties(systemProperties);

        EmbedderLoggerImpl logger =
            new EmbedderLoggerImpl( listener, debugMavenEmbedder ? org.codehaus.plexus.logging.Logger.LEVEL_DEBUG
                            : org.codehaus.plexus.logging.Logger.LEVEL_INFO );
        mavenRequest.setMavenLoggerManager( logger );
        
        ClassLoader mavenEmbedderClassLoader = new MaskingClassLoader( MavenUtil.class.getClassLoader() );

        {// are we loading the right components.xml? (and not from Maven that's running Jetty, if we are running in "mvn hudson-dev:run" or "mvn hpi:run"?
            Enumeration<URL> e = mavenEmbedderClassLoader.getResources("META-INF/plexus/components.xml");
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                LOGGER.fine("components.xml from "+url);
            }
        }

        mavenRequest.setProcessPlugins( false );
        mavenRequest.setResolveDependencies( false );
        mavenRequest.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 );
         
        return mavenRequest;
    }
    
    /**
     * {@link MavenEmbedderLogger} implementation that
     * sends output to {@link TaskListener}.
     * 
     * @author Kohsuke Kawaguchi
     */
    private static final class EmbedderLoggerImpl extends MavenLoggerManager {
        private final PrintStream logger;

        public EmbedderLoggerImpl(TaskListener listener, int threshold) {
            super(new ConsoleLogger( threshold, "hudson-logger" ));
            logger = listener.getLogger();
        }

        private void print(String message, Throwable throwable, int threshold, String prefix) {
            if (getThreshold() <= threshold) {
                StringTokenizer tokens = new StringTokenizer(message,"\n");
                while(tokens.hasMoreTokens()) {
                    logger.print(prefix);
                    logger.println(tokens.nextToken());
                }

                if (throwable!=null)
                    throwable.printStackTrace(logger);
            }
        }

        public void debug(String message, Throwable throwable) {
            print(message, throwable, org.codehaus.plexus.logging.Logger.LEVEL_DEBUG, "[DEBUG] ");
        }

        public void info(String message, Throwable throwable) {
            print(message, throwable, org.codehaus.plexus.logging.Logger.LEVEL_INFO, "[INFO ] ");
        }

        public void warn(String message, Throwable throwable) {
            print(message, throwable, org.codehaus.plexus.logging.Logger.LEVEL_WARN, "[WARN ] ");
        }

        public void error(String message, Throwable throwable) {
            print(message, throwable, org.codehaus.plexus.logging.Logger.LEVEL_ERROR, "[ERROR] ");
        }

        public void fatalError(String message, Throwable throwable) {
            print(message, throwable, org.codehaus.plexus.logging.Logger.LEVEL_FATAL, "[FATAL] ");
        }
    }

    /**
     * When we run in Jetty during development, embedded Maven will end up
     * seeing some of the Maven class visible through Jetty, and this confuses it.
     *
     * <p>
     * Specifically, embedded Maven will find all the component descriptors
     * visible through Jetty, yet when it comes to loading classes, classworlds
     * still load classes from local realms created inside embedder.
     *
     * <p>
     * This classloader prevents this issue by hiding the component descriptor
     * visible through Jetty.
     */
    private static final class MaskingClassLoader extends ClassLoader {

        public MaskingClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Enumeration<URL> getResources(String name) throws IOException {
            final Enumeration<URL> e = super.getResources(name);
            return new Enumeration<URL>() {
                URL next;

                public boolean hasMoreElements() {
                    fetch();
                    return next!=null;
                }

                public URL nextElement() {
                    fetch();
                    URL r = next;
                    next = null;
                    return r;
                }

                private void fetch() {
                    while(next==null && e.hasMoreElements()) {
                        next = e.nextElement();
                        if(shouldBeIgnored(next))
                            next = null;
                    }
                }

                private boolean shouldBeIgnored(URL url) {
                    String s = url.toExternalForm();
                    if(s.contains("maven-plugin-tools-api"))
                        return true;
                    // because RemoteClassLoader mangles the path, we can't check for plexus/components.xml,
                    // which would have otherwise made the test cheaper.
                    if(s.endsWith("components.xml")) {
                        BufferedReader r=null;
                        try {
                            // is this designated for interception purpose? If so, don't load them in the MavenEmbedder
                            // earlier I tried to use a marker file in the same directory, but that won't work
                            r = new BufferedReader(new InputStreamReader(url.openStream()));
                            for (int i=0; i<2; i++) {
                                String l = r.readLine();
                                if(l!=null && l.contains("MAVEN-INTERCEPTION-TO-BE-MASKED"))
                                    return true;
                            }
                        } catch (IOException _) {
                            // let whoever requesting this resource re-discover an error and report it
                        } finally {
                            IOUtils.closeQuietly(r);
                        }
                    }
                    return false;
                }
            };
        }
    }
    
    /**
     * If set to true, maximize the logging level of Maven embedder.
     */
    public static boolean debugMavenEmbedder = Boolean.getBoolean( "debugMavenEmbedder" );

    private static final Logger LOGGER = Logger.getLogger(MavenUtil.class.getName());
}
