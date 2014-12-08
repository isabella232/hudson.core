/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 * Kohsuke Kawaguchi, Winston Prakash
 *
 *
 ******************************************************************************
 */
package hudson;

import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.IOException2;
import hudson.util.Scrambler;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import com.thoughtworks.xstream.XStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import org.apache.commons.codec.binary.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP proxy configuration.
 *
 * <p> Use {@link #open(URL)} to open a connection with the proxy setting. <p>
 * Proxy Authorization is done via "Proxy-Authorization" HTTP header See
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html section 14.34</a>).
 *
 * @see Hudson#proxy
 */
public final class ProxyConfiguration implements Saveable {

    public String name;
    public int port;
    public String noProxyFor;
    private int TIME_OUT_RETRY_COUNT = 5;
    private int CONNECTION_TIME_OUT_MS = 2000;
    private transient Logger logger = LoggerFactory.getLogger(ProxyConfiguration.class);
    /**
     * Possibly null proxy user name and password. Password is base64 scrambled
     * since this is persisted to disk.
     */
    private String userName;
    private String password;
    private boolean authNeeded = false;
    private File rootDir;

    public ProxyConfiguration(File dir) throws IOException {
        rootDir = dir;
        load();
    }

    public ProxyConfiguration(String name, int port) {
        this(name, port, null, null, null, false);
    }

    public ProxyConfiguration(String name, int port, String noProxyFor, String userName, String password, boolean authNeeded) {
        configure(name, port, noProxyFor, userName, password, authNeeded);
    }

    public void configure(String name, int port, String noProxyFor, String userName, String password, boolean authNeeded) {
        this.name = name;
        this.port = port;
        this.userName = userName;
        this.password = Scrambler.scramble(password);
        this.authNeeded = authNeeded;
        this.noProxyFor = noProxyFor;

    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getNoProxyFor() {
        return noProxyFor;
    }

    public boolean isAuthNeeded() {
        return authNeeded;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return Scrambler.descramble(password);
    }

    public Proxy createProxy() {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(name, port));
    }

    @Override
    public void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }
        getXmlFile().write(this);
        // Could be called oiutside of Hudson Context (Ex Initial Setup)
        if (Hudson.getInstance() != null) {
            SaveableListener.fireOnChange(this, getXmlFile());
        }
    }

    public XmlFile getXmlFile() {
        return new XmlFile(XSTREAM, new File(rootDir, "proxy.xml"));
    }

    public void load() throws IOException {
        XmlFile config = getXmlFile();
        try {
            if (config.exists()) {
                config.unmarshal(this);
            }
        } catch (IOException e) {
            logger.error("Failed to load " + config, e);
        }
    }

    public URLConnection openUrl(URL url) throws IOException {

        if (name == null) {
            URLConnection urlConnection = url.openConnection();
            connect(urlConnection);
            return urlConnection;
        }

        if (noProxyFor != null) {
            StringTokenizer tokenizer = new StringTokenizer(noProxyFor, ",");
            while (tokenizer.hasMoreTokens()) {
                String noProxyHost = tokenizer.nextToken().trim();
                if (noProxyHost.contains("*")) {
                    if (url.getHost().trim().contains(noProxyHost.replaceAll("\\*", ""))) {
                        return url.openConnection(Proxy.NO_PROXY);
                    }
                } else if (url.getHost().trim().equals(noProxyHost)) {
                    return url.openConnection(Proxy.NO_PROXY);
                }
            }
        }

        URLConnection urlConnection = url.openConnection(createProxy());

        if (isAuthNeeded()) {
            String credentials = getUserName() + ":" + getPassword();
            String encoded = new String(Base64.encodeBase64(credentials.getBytes()));
            urlConnection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
        }
        connect(urlConnection);
        return urlConnection;
    }

    private void connect(URLConnection urlConnection) throws IOException {
        boolean connected = false;
        int count = 0;
        urlConnection.setConnectTimeout(CONNECTION_TIME_OUT_MS);
        while (!connected) {
            try {
                urlConnection.connect();
                connected = true;
            } catch (SocketTimeoutException exc) {
                logger.debug("Connection timed out. trying again {}", count);
                if (++count > TIME_OUT_RETRY_COUNT) {
                    throw new IOException(
                            "Could not connect to " + urlConnection.getURL().toExternalForm() + ". Connection timed out after " + TIME_OUT_RETRY_COUNT + " tries.");
                }
                connected = false;
            } catch (UnknownHostException exc) {
                throw new IOException2(
                        "Could not connect to " + urlConnection.getURL().toExternalForm() + ". Check your internet connection.",
                        exc);
            } catch (ConnectException exc) {
                throw new IOException2(
                        "Could not connect to " + urlConnection.getURL().toExternalForm() + ". Check your internet connection.",
                        exc);
            }
        }

    }

    /**
     * This method should be used wherever {@link URL#openConnection()} to
     * internet URLs is invoked directly.
     */
    public static URLConnection open(URL url) throws IOException {
        Hudson hudson = Hudson.getInstance(); // this code might run on slaves
        ProxyConfiguration proxyConfig = hudson != null ? hudson.proxy : null;


        if (proxyConfig == null) {
            return url.openConnection();
        } else {
            return proxyConfig.openUrl(url);
        }

    }
    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("proxy", ProxyConfiguration.class);
    }
}
