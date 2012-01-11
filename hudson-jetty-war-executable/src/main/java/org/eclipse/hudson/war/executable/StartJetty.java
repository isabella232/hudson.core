/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *    Winston Prakash
 *     
 *
 *******************************************************************************/
package org.eclipse.hudson.war.executable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.Manifest;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Simple boot class to make the war executable
 * @author Winston Prakash
 */
public class StartJetty {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--httpPort=")) {
                String portStr = args[i].substring("--httpPort=".length());
                port = Integer.parseInt(portStr);
            }else if (args[i].startsWith("--version")) {
                System.out.println("Hudson Continuous Integration Server" + getVersion());
                System.exit(0);
            }else if (args[i].startsWith("--usage")) {
                String usageStr = "Hudson Continuous Integration Server " + getVersion() + "\n" +
                "Usage: java -jar hudson.war [--option=value] [--option=value] ... \n" +
                "\n" +
                "Options:\n" +
                "   --httpPort=<value>   HTTP listening port. Default value is 8080\n" +
                "   --version            Show Hudson version and quit\n";
                System.out.println(usageStr);
                System.exit(0);
            }
        }

        Server server = new Server(port);
        SocketConnector connector = new SocketConnector();

        ProtectionDomain protectionDomain = StartJetty.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();

        WebAppContext context = new WebAppContext();

        File tempDir = new File(getHomeDir(), "war");
        tempDir.mkdirs();
        context.setTempDirectory(tempDir);

        context.setContextPath("/");
        context.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        context.setServer(server);
        context.setWar(location.toExternalForm());
        
        // This is used by Windows Service Installer in Hudson Management 
        System.out.println("War location - " + location.getPath());
        System.setProperty("executable-war", location.getPath());

        server.addHandler(context);
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

    private static String getVersion() throws IOException {
        Enumeration manifests = StartJetty.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            URL manifestUrl = (URL) manifests.nextElement();
            Manifest manifest = new Manifest(manifestUrl.openStream());
            String hudsonVersion = manifest.getMainAttributes().getValue("Hudson-Version");
            if (hudsonVersion != null) {
                return hudsonVersion;
            }
        }
        return "Unknown Version";
    }
}
