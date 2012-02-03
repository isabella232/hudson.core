/**
 * *****************************************************************************
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
 * Winston Prakash
 *
 ******************************************************************************
 */
package org.eclipse.hudson.jetty;

import java.io.File;
import java.net.URL;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Jetty Utility to launch the Jetty Server
 *
 * @author Winston Prakash
 */
public class JettyLauncher {

    private static String contextPath = "/";

    public static void start(String[] args, URL warUrl) throws Exception {

        int port = 8080;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--httpPort=")) {
                String portStr = args[i].substring("--httpPort=".length());
                port = Integer.parseInt(portStr);
            } else if (args[i].startsWith("--prefix=")) {
                contextPath = "/" + args[i].substring("--prefix=".length());
            }
        }

        final Server server = new Server(port);
        SocketConnector connector = new SocketConnector();

        WebAppContext context = new WebAppContext();

        File tempDir = new File(getHomeDir(), "war");
        tempDir.mkdirs();
        context.setTempDirectory(tempDir);

        context.setContextPath(contextPath);
        context.setDescriptor(warUrl.toExternalForm() + "/WEB-INF/web.xml");
        context.setServer(server);
        context.setWar(warUrl.toExternalForm());

        // This is used by Windows Service Installer in Hudson Management 
        System.out.println("War - " + warUrl.getPath());
        System.setProperty("executable-war", warUrl.getPath());

        server.addHandler(context);
        server.setStopAtShutdown(true);

        server.start();
        server.join();
    }

    /**
     * Get the home directory for Hudson.
     */
    private static File getHomeDir() {

        // Check HUDSON_HOME  system property
        String hudsonHomeProperty = System.getProperty("HUDSON_HOME");
        if (hudsonHomeProperty != null) {
            return new File(hudsonHomeProperty.trim());
        }

        // Check if the environment variable is et
        try {
            String hudsonHomeEnv = System.getenv("HUDSON_HOME");
            if (hudsonHomeEnv != null) {
                return new File(hudsonHomeEnv.trim()).getAbsoluteFile();
            }
        } catch (Throwable _) {
            // Some JDK could throw error if HUDSON_HOME is not set.
            // Ignore and fall through
        }

        // Default hudson home 
        return new File(new File(System.getProperty("user.home")), ".hudson");
    }
}
