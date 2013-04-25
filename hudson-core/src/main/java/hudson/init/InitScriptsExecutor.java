/**
 * *****************************************************************************
 *
 * Copyright (c) 2013, Oracle Corporation
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Winston Prakash, Kohsuke Kawaguchi
 *
 ******************************************************************************
 */
package hudson.init;

import static hudson.init.InitMilestone.JOB_LOADED;
import hudson.model.Hudson;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.hudson.script.ScriptSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitScriptsExecutor {

    private  static Logger logger = LoggerFactory.getLogger(InitScriptsExecutor.class);

    @Initializer(after=JOB_LOADED)
    public static void init(Hudson hudson) throws IOException {
        URL bundledInitScript = hudson.servletContext.getResource("/WEB-INF/init.groovy");
        if (bundledInitScript != null) {
            logger.info("Executing bundled init script: " + bundledInitScript);
            InputStream in = bundledInitScript.openStream();
            try {
                String script = IOUtils.toString(in);
                logger.info(new Script(script).execute());
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        File initScript = new File(hudson.getRootDir(), "init.groovy");
        if (initScript.exists()) {
            execute(initScript);
        }

        File initScriptD = new File(hudson.getRootDir(), "init.groovy.d");
        if (initScriptD.isDirectory()) {
            File[] scripts = initScriptD.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".groovy");
                }
            });
            if (scripts != null) {
                // sort to run them in a deterministic order
                Arrays.sort(scripts);
                for (File f : scripts) {
                    execute(f);
                }
            }
        }
    }

    private static void execute(File initScript) throws IOException {
        logger.info("Executing " + initScript);
        String script = FileUtils.readFileToString(initScript);
        logger.info(new Script(script).execute());
    }

    private static final class Script {

        private final String script;
        private transient ClassLoader parentClassLoader;
        private ScriptSupport scriptSupport;

        private Script(String script) {
            this(script, ScriptSupport.SCRIPT_GROOVY);
        }

        private Script(String script, String scriptType) {
            this.script = script;
            parentClassLoader = getClassLoader();
            if (scriptType != null) {
                for (ScriptSupport scriptSupport : ScriptSupport.getAvailableScriptSupports()) {
                    if (scriptSupport.hasSupport(scriptType)) {
                        this.scriptSupport = scriptSupport;
                    }
                }
            }
        }

        private Script(String script, ScriptSupport scriptSupport) {
            this(script);
            this.scriptSupport = scriptSupport;
        }

        public ClassLoader getClassLoader() {
            return Hudson.getInstance().getPluginManager().uberClassLoader;
        }

        public String execute() throws RuntimeException {
            if (scriptSupport != null) {
                StringWriter out = new StringWriter();
                PrintWriter printWriter = new PrintWriter(out);
                scriptSupport.evaluate(parentClassLoader, script, null, printWriter);
                return out.toString();
            } else {
                return "No script support to execute the script. Install script support plugin";
            }
        }
    }
}
