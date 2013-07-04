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

package hudson.lifecycle;

import hudson.FilePath;
import hudson.model.Hudson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

/**
 * Allow restart by means of a <code>--restartCommand</code> command line option
 * or a <code>hudson-restart[.extension]</code> file in <code>HUDSON_HOME</code>.
 * @author Bob Foster
 */
public class RestartCommandLifecycle extends Lifecycle {
    
    private static final Logger LOGGER = Logger.getLogger(RestartCommandLifecycle.class.getName());
    
    private static final String HUDSON_RESTART_SCRIPT_NAME = "hudson-restart";
    private static final String HUDSON_RESTART_COMMAND_KEY = "hudson.restart";
    
    private static String restartCommand = null;
    private static File restartScript = null;
    
    private static String getExtension(String name) {
        int ext = name.lastIndexOf('.');
        if (ext > 0) {
            return name.substring(ext);
        }
        return "";
    }
    
    private static class RestartFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            String name = pathname.getName();
            String extension = getExtension(name);
            return HUDSON_RESTART_SCRIPT_NAME.equals(name.substring(0, name.length()-extension.length()));
        }
    }
    
    private static boolean checkReturn() {
        String hl = System.getProperty("hudson.lifecycle");
        if (hl != null) {
            LOGGER.log(WARNING, "hudson.lifecycle specified, "+(restartCommand != null ? HUDSON_RESTART_COMMAND_KEY : HUDSON_RESTART_SCRIPT_NAME+" script")+" ignored");
            return false;
        }
        return true;
    }

    public static boolean isConfigured() {
        String p = System.getProperty(HUDSON_RESTART_COMMAND_KEY);
        if (p != null) {
            restartCommand = p;
            return checkReturn();
        } else {
            File home = Hudson.getInstance().getRootDir();
            File[] restartScripts = home.listFiles(new RestartFilter());
            int numScripts = restartScripts.length;
            if (numScripts == 0) {
                return false;
            }
            restartScript = restartScripts[0];
            if (numScripts > 1) {
                LOGGER.log(WARNING, "More than one "+HUDSON_RESTART_SCRIPT_NAME+" script, using " + restartScript.getName());
            }
            return checkReturn();
        }
    }

    public void restart() throws IOException, InterruptedException {
        // Keep it simple
        // Try to avoid broken pipe exception by not opening any streams
        String cmd = restartCommand;
        if (cmd == null) {
            cmd = restartScript.getCanonicalPath();
        }
        LOGGER.log(INFO, "Executing restart command: "+cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process process = pb.start();
        int ret = process.waitFor();
        if (ret != 0) {
            throw new IOException("Restart command '"+restartCommand+"' failed with return code "+ret);
        }
    }

    /**
     * Can the {@link #restart()} method restart Hudson?
     *
     * @throws RestartNotSupportedException If the restart is not supported,
     * throw this exception and explain the cause.
     */
    public void verifyRestartable() throws RestartNotSupportedException {
    }

    /**
     * The same as {@link #verifyRestartable()} except the status is indicated
     * by the return value, not by an exception.
     */
    public boolean canRestart() {
        return true;
    }
}
