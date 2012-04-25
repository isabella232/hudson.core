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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Simple boot class to make the war executable
 *
 * @author Winston Prakash
 */
public class Executable {

    private final String[] jettyJars = {
        "libs/jetty.jar",
        "libs/jetty-util.jar",
        "libs/jetty-servlet-api.jar",
        "libs/hudson-jetty-war-executable.jar"
    };
    private List<String> arguments;

    public static void main(String[] args) throws Exception {

        String javaVersion = System.getProperty("java.version");

        StringTokenizer tokens = new StringTokenizer(javaVersion, ".-_");

        int majorVersion = Integer.parseInt(tokens.nextToken());
        int minorVersion = Integer.parseInt(tokens.nextToken());

        // Make sure Java version is 1.6 or later
        if (majorVersion < 2) {
            if (minorVersion < 6) {
                System.err.println("Hudson requires Java 6 or later.");
                System.err.println("Your java version is " + javaVersion);
                System.err.println("Java Home:  " + System.getProperty("java.home"));
                System.exit(0);
            }
        }

        Executable executable = new Executable();
        executable.parseArguments(args);
        executable.launchJetty();
    }

    private void parseArguments(String[] args) throws IOException {
        arguments = Arrays.asList(args);

        for (String arg : arguments) {
            if (arg.startsWith("--version")) {
                System.out.println("Hudson Continuous Integration Server" + getHudsonVersion());
                System.exit(0);
            } else if (arg.startsWith("--usage")) {
                printUsage();
                break;
            } else if (arg.startsWith("--logfile=")) {
                String logFile = arg.substring("--logfile=".length());
                System.out.println("Logging information is send to file " + logFile);
                FileOutputStream fos = new FileOutputStream(new File(logFile));
                PrintStream ps = new PrintStream(fos);
                System.setOut(ps);
                System.setErr(ps);
                break;
            }
        }
    }

    private void printUsage() throws IOException {
        String usageStr = "Hudson Continuous Integration Server " + getHudsonVersion() + "\n"
                + "Usage: java -jar hudson.war [--option=value] [--option=value] ... \n"
                + "\n"
                + "Options:\n"
                + "   --version                        Show Hudson version and quit\n"
                + "   --logfile=<filename>             Send the output log to this file\n"
                + "   --prefix=<prefix-string>         Add this prefix to all URLs (eg http://localhost:8080/prefix/resource). Default is none\n\n"
                + "   --httpPort=<value>               HTTP listening port. Default value is 8080\n\n"
                + "   --httpsPort=<value>              HTTPS listening port. Disabled by default\n"
                + "   --httpsKeyStore=<filepath>       Location of the SSL KeyStore file.\n"
                + "   --httpsKeyStorePassword=<value>  Password for the SSL KeyStore file\n\n"
                + "   --updateServer=<your server>     Specify your own update server (eg http://updates.mycompany.com/).\n"
                + "                                    For details see http://wiki.hudson-ci.org/Alternate+Update+Server\n\n"
                + "   --disableUpdateCenterSwitch      Disable the ability to specify alternate Update Center URL via Plugin Manager Advanced tab\n\n";
        
        System.out.println(usageStr);
        System.exit(0);
    }

    private void launchJetty() throws Exception {
        ProtectionDomain protectionDomain = Executable.class.getProtectionDomain();
        URL warUrl = protectionDomain.getCodeSource().getLocation();
        
        // For Testing purpose
//        File file = new File("/Users/winstonp/hudson/hudson-eclipse/org.eclipse.hudson.core/hudson-war/target/hudson-war-3.0.0-SNAPSHOT.war");
//        URL warUrl = file.toURI().toURL();
        
        System.out.println(warUrl.getPath());

        List<URL> jarUrls = extractJettyJarsFromWar(warUrl.getPath());

        ClassLoader urlClassLoader = new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]));
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        Class jettyUtil = urlClassLoader.loadClass("org.eclipse.hudson.jetty.JettyLauncher");
        Method mainMethod = jettyUtil.getMethod("start", new Class[]{String[].class, URL.class});
        mainMethod.invoke(null, new Object[]{arguments.toArray(new String[arguments.size()]), warUrl});
    }

    /**
     * Find the Hudson version from war manifest
     *
     * @return
     * @throws IOException
     */
    private static String getHudsonVersion() throws IOException {
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
                //System.out.println("Extracted " + entryPath + " to " + tmpFile);
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
