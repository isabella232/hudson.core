/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *******************************************************************************/ 

package hudson;

import hudson.model.Hudson;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers a DNS multi-cast service-discovery support.
 *
 * @author Kohsuke Kawaguchi
 */
public class DNSMultiCast implements Closeable {

    private JmDNS jmdns;

    public DNSMultiCast(Hudson hudson) {
        if (disabled) {
            return; // escape hatch
        }
        try {
            this.jmdns = JmDNS.create();

            Map<String, String> props = new HashMap<String, String>();
            String rootURL = hudson.getRootUrl();
            if (rootURL != null) {
                props.put("url", rootURL);
            }
            try {
                props.put("version", String.valueOf(Hudson.getVersion()));
            } catch (IllegalArgumentException e) {
                // failed to parse the version number
            }

            TcpSlaveAgentListener tal = hudson.getTcpSlaveAgentListener();
            if (tal != null) {
                props.put("slave-port", String.valueOf(tal.getPort()));
            }

            jmdns.registerService(ServiceInfo.create("_hudson._tcp.local.", "hudson",
                    80, 0, 0, props));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to advertise the service to DNS multi-cast", e);
        }
    }

    public void close() {
        if (jmdns != null) {
            try {
                jmdns.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            jmdns = null;
        }
    }
    private static final Logger LOGGER = Logger.getLogger(DNSMultiCast.class.getName());
    public static boolean disabled = !Boolean.getBoolean(DNSMultiCast.class.getName() + ".enabled");
}
