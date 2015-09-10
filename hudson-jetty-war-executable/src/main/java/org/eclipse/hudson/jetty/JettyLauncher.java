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
 * Winston Prakash, Duncan Mills
 *
 ******************************************************************************
 */
package org.eclipse.hudson.jetty;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Jetty Utility to launch the Jetty Server
 *
 * @author Winston Prakash
 */
public class JettyLauncher {

    private static String contextPath = "/";

    public static void start(String[] args, URL warUrl) throws Exception {

        String httpListenAddress = "0.0.0.0";
        String httpsListenAddress = "0.0.0.0";

        int httpPort = 8080;
        int httpsPort = -1;

        String keyStorePath = null;
        String keyStorePassword = null;
        String keyManagerPassword = null;

        String updateServer = null;

        boolean disableUpdateCenterSwitch = false;
        boolean skipInitSetup = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--httpListenAddress=")) {
                httpListenAddress = args[i].substring("--httpListenAddress=".length());
            }

            if (args[i].startsWith("--httpsListenAddress=")) {
                httpsListenAddress = args[i].substring("--httpsListenAddress=".length());
            }

            if (args[i].startsWith("--httpPort=")) {
                String portStr = args[i].substring("--httpPort=".length());
                httpPort = Integer.parseInt(portStr);
            }

            if (args[i].startsWith("--httpsPort=")) {
                String portStr = args[i].substring("--httpsPort=".length());
                httpsPort = Integer.parseInt(portStr);
            }

            if (args[i].startsWith("--httpsKeyStore=")) {
                keyStorePath = args[i].substring("--httpsKeyStore=".length());
            }

            if (args[i].startsWith("--httpsKeyStorePassword=")) {
                keyStorePassword = args[i].substring("--httpsKeyStorePassword=".length());
            }

            if (args[i].startsWith("--httpsKeyManagerPassword=")) {
                keyManagerPassword = args[i].substring("--httpsKeyManagerPassword=".length());
            }

            if (args[i].startsWith("--prefix=")) {
                String prefix = args[i].substring("--prefix=".length());
                if (prefix.startsWith("/")) {
                    contextPath = prefix;
                } else {
                    contextPath = "/" + prefix;
                }
            }
            if (args[i].startsWith("--updateServer=")) {
                updateServer = args[i].substring("--updateServer=".length());
            }

            if (args[i].startsWith("--disableUpdateCenterSwitch")) {
                disableUpdateCenterSwitch = true;
            }

            if (args[i].startsWith("--skipInitSetup")) {
                skipInitSetup = true;
            }
        }

        Server server = new Server();

        List<Connector> connectors = new ArrayList<Connector>();

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setOutputBufferSize(32768);

        ServerConnector httpConnector = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        httpConnector.setHost(httpListenAddress);
        httpConnector.setPort(httpPort);
        httpConnector.setIdleTimeout(30000);

        connectors.add(httpConnector);

        if (httpsPort != -1) {

            SslContextFactory sslContextFactory = new SslContextFactory();

            if (keyStorePath != null) {
                sslContextFactory.setKeyStorePath(keyStorePath);
            }
            if (keyStorePassword != null) {
                sslContextFactory.setKeyStorePassword(keyStorePassword);
            }

            if (keyManagerPassword != null) {
                sslContextFactory.setKeyManagerPassword(keyManagerPassword);
            }

            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());
            https_config.setOutputBufferSize(32768);

            ServerConnector httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https_config));
            httpsConnector.setHost(httpsListenAddress);
            httpsConnector.setPort(httpsPort);
            httpsConnector.setIdleTimeout(500000);

            connectors.add(httpsConnector);
        }

        server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

        WebAppContext context = new WebAppContext();

        File tempDir = new File(getHomeDir(), "war");
        if (tempDir.exists()) {
            deleteFileRecursively(tempDir);
        }
        tempDir.mkdirs();
        context.setTempDirectory(tempDir);

        context.setContextPath(contextPath);
        context.setDescriptor(warUrl.toExternalForm() + "/WEB-INF/web.xml");
        context.setServer(server);
        context.setWar(warUrl.toExternalForm());

        LoginService loginService = new HashLoginService("defaultLoginService");
        context.getSecurityHandler().setLoginService(loginService);

        // This is used by Windows Service Installer in Hudson Management 
        System.out.println("War - " + warUrl.getPath());
        System.setProperty("executable-war", warUrl.getPath());

        if (updateServer != null) {
            System.setProperty("updateServer", updateServer);
        }

        if (disableUpdateCenterSwitch) {
            System.setProperty("hudson.pluginManager.disableUpdateCenterSwitch", "true");
        }

        if (skipInitSetup) {
            System.setProperty("skipInitSetup", "true");
        }

        server.setHandler(context);
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

    private static boolean deleteFileRecursively(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (File file : files) {
                deleteFileRecursively(file);
            }
        }
        boolean deleted = path.delete();
        if (!deleted) {
            System.out.println("Failed to delete - " + path);
        }
        return deleted;
    }
}
