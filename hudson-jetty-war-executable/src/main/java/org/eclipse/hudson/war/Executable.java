package org.eclipse.hudson.war;

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
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Simple boot class to make the war executable
 *
 * @author Winston Prakash
 */
public class Executable {

    private String[] jettyJars = {
        "libs/jetty.jar",
        "libs/jetty-util.jar",
        "libs/jetty-servlet-api.jar",
        "libs/hudson-jetty-war-executable.jar"
    };

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--version")) {
                System.out.println("Hudson Continuous Integration Server" + getVersion());
                System.exit(0);
            } else if (args[i].startsWith("--usage")) {
                String usageStr = "Hudson Continuous Integration Server " + getVersion() + "\n"
                        + "Usage: java -jar hudson.war [--option=value] [--option=value] ... \n"
                        + "\n"
                        + "Options:\n"
                        + "   --httpPort=<value>   HTTP listening port. Default value is 8080\n"
                        + "   --version            Show Hudson version and quit\n";
                System.out.println(usageStr);
                System.exit(0);
            }
        }

        Executable jettyLauncher = new Executable();
        jettyLauncher.launchJetty(args);
    }

    private void launchJetty(String[] args) throws Exception {
        ProtectionDomain protectionDomain = Executable.class.getProtectionDomain();
        URL warUrl = protectionDomain.getCodeSource().getLocation();

        List<URL> jarUrls = extractJettyJarsFromWar(warUrl.getPath());

        ClassLoader urlClassLoader = new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]));
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        Class jettyUtil = urlClassLoader.loadClass("org.eclipse.hudson.jetty.JettyLauncher");
        Method mainMethod = jettyUtil.getMethod("start", new Class[]{String[].class, URL.class});
        mainMethod.invoke(null, new Object[]{args, warUrl});
    }

    /**
     * Find the Hudson version from war manifest
     *
     * @return
     * @throws IOException
     */
    private static String getVersion() throws IOException {
        Enumeration manifests = Executable.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
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

    /**
     * Extract the Jetty Jars from the war
     *
     * @throws IOException
     */
    private List<URL> extractJettyJarsFromWar(String warPath) throws IOException {

        System.out.println(warPath);

        JarFile jarFile = new JarFile(warPath);



        List<URL> jarUrls = new ArrayList<URL>();

        InputStream inStream = null;

        try {

            for (String entryPath : jettyJars) {

                File tmpFile;
                try {
                    tmpFile = File.createTempFile(entryPath.replaceAll("/", "_"), "hudson");
                } catch (IOException e) {
                    String tmpdir = System.getProperty("java.io.tmpdir");
                    throw new IOException("Failed to extract " + entryPath + " to " + tmpdir, e);
                }
                JarEntry jarEntry = jarFile.getJarEntry(entryPath);
                inStream = jarFile.getInputStream(jarEntry);

                OutputStream outStream = new FileOutputStream(tmpFile);
                try {
                    byte[] buffer = new byte[8192];
                    int readLength;
                    while ((readLength = inStream.read(buffer)) > 0) {
                        outStream.write(buffer, 0, readLength);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                } finally {
                    outStream.close();
                }

                tmpFile.deleteOnExit();
                System.out.println("Extracted " + entryPath + " to " + tmpFile);
                jarUrls.add(tmpFile.toURI().toURL());
            }

        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (inStream != null) {
                inStream.close();
            }
        }

        return jarUrls;
    }
}
