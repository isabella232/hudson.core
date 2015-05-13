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
*    Kohsuke Kawaguchi, Tom Huybrechts, Nikita Levyankov
 *
 *
 *******************************************************************************/ 

package hudson.tasks;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Common part between {@link Shell} and {@link BatchFile}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CommandInterpreter extends Builder {

    /**
     * Command to execute. The format depends on the actual
     * {@link CommandInterpreter} implementation.
     */
    protected final String command;

    public CommandInterpreter(String command) {
        this.command = command;
    }

    public final String getCommand() {
        return command;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        if (isDisabled()){
            listener.getLogger().print("\nThe command interpreter builder is temporarily disabled.\n");
            return true;
        }
        return perform(build, launcher, (TaskListener) listener);
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
        if (isDisabled()){
            // just continue, this builder is disabled temporarily
            return true;
        }
        FilePath ws = build.getWorkspace();
        FilePath script = null;
        try {
            try {
                script = createScriptFile(ws);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError(Messages.CommandInterpreter_UnableToProduceScript()));
                return false;
            }

            int r;
            try {
                EnvVars envVars = build.getEnvironment(listener);
                // on Windows environment variables are converted to all upper case,
                // but no such conversions are done on Unix, so to make this cross-platform,
                // convert variables to all upper cases.
                for (Map.Entry<String, String> e : build.getBuildVariables().entrySet()) {
                    envVars.put(e.getKey(), e.getValue());
                }

                r = launcher.launch().cmds(buildCommandLine(script)).envs(envVars).stdout(listener).pwd(ws).join();
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError(Messages.CommandInterpreter_CommandFailed()));
                r = -1;
            }
            return r == 0;
        } finally {
            try {
                if (script != null) {
                    script.delete();
                }
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError(Messages.CommandInterpreter_UnableToDelete(script)));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CommandInterpreter that = (CommandInterpreter) o;
        return StringUtils.equalsIgnoreCase(command, that.command);
    }

    @Override
    public int hashCode() {
        return command != null ? command.hashCode() : 0;
    }

    /**
     * Creates a script file in a temporary name in the specified directory.
     */
    public FilePath createScriptFile(FilePath dir) throws IOException, InterruptedException {
        return dir.createTextTempFile("hudson", getFileExtension(), getContents(), false);
    }

    public abstract String[] buildCommandLine(FilePath script);

    protected abstract String getContents();

    protected abstract String getFileExtension();
}
