/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *    Kohsuke Kawaguchi
 *     
 *
 *******************************************************************************/ 

package hudson.os;

import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Launcher;
import hudson.remoting.LocalChannel;
import hudson.remoting.VirtualChannel;
import hudson.remoting.Which;
import hudson.slaves.Channels;
import hudson.util.ArgumentListBuilder;
import hudson.util.jna.NativeAccessException;
import hudson.util.jna.NativeUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

/**
 * Executes {@link Callable} as the super user, by forking a new process and executing the closure in there
 * if necessary.
 *
 * <p>
 * A best effort is made to execute the closure as root, but we may still end up exeucting the closure
 * in the non-root privilege, so the closure should expect that and handle it gracefully.
 *
 * <p>
 * Still very much experimental. Subject to change. <b>Don't use it.</b>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SU {
    private SU() { // not meant to be instantiated
    }

    /**
     * Returns a {@link VirtualChannel} that's connected to the priviledge-escalated environment.
     *
     * @return
     *      Never null. This may represent a channel to a separate JVM, or just {@link LocalChannel}.
     *      Close this channel and the SU environment will be shut down.
     */
    public static VirtualChannel start(final TaskListener listener, final String rootUsername, final String rootPassword) throws IOException, InterruptedException {
        if(File.pathSeparatorChar==';') // on Windows
            return newLocalChannel();  // TODO: perhaps use RunAs to run as an Administrator?

        String os = Util.fixNull(System.getProperty("os.name"));
        if(os.equals("Linux"))
            return new UnixSu() {
                protected String sudoExe() {
                    return "sudo";
                }

                protected Process sudoWithPass(ArgumentListBuilder args) throws IOException {
                    args.prepend(sudoExe(),"-S");
                    listener.getLogger().println("$ "+Util.join(args.toList()," "));
                    ProcessBuilder pb = new ProcessBuilder(args.toCommandArray());
                    Process p = pb.start();
                    // TODO: use -p to detect prompt
                    // TODO: detect if the password didn't work
                    PrintStream ps = new PrintStream(p.getOutputStream());
                    ps.println(rootPassword);
                    ps.println(rootPassword);
                    ps.println(rootPassword);
                    return p;
                }
            }.start(listener,rootPassword);

        if(os.equals("SunOS"))
            return new UnixSu() {
                protected String sudoExe() {
                    return "/usr/bin/pfexec";
                }

                protected Process sudoWithPass(ArgumentListBuilder args) throws IOException {
                    listener.getLogger().println("Running with embedded_su");
                    ProcessBuilder pb = new ProcessBuilder(args.prepend(sudoExe()).toCommandArray());
                    return EmbeddedSu.startWithSu(rootUsername, rootPassword, pb);
                }
            // in solaris, pfexec never asks for a password, so username==null means
            // we won't be using password. this helps disambiguate empty password
            }.start(listener,rootUsername==null?null:rootPassword);

        // TODO: Mac?

        // unsupported platform, take a chance
        return newLocalChannel();
    }

    private static LocalChannel newLocalChannel() {
        return new LocalChannel(Computer.threadPoolForRemoting);
    }

    /**
     * Starts a new priviledge-escalated environment, execute a closure, and shut it down.
     */
    public static <V,T extends Throwable> V execute(TaskListener listener, String rootUsername, String rootPassword, final Callable<V, T> closure) throws T, IOException, InterruptedException {
        VirtualChannel ch = start(listener, rootUsername, rootPassword);
        try {
            return ch.call(closure);
        } finally {
            ch.close();
            ch.join(3000); // give some time for orderly shutdown, but don't block forever.
        }
    }

    private static abstract class UnixSu {

        protected abstract String sudoExe();

        protected abstract Process sudoWithPass(ArgumentListBuilder args) throws IOException;

        VirtualChannel start(TaskListener listener, String rootPassword) throws IOException, InterruptedException {
            final int uid;
            try {
                uid = NativeUtils.getInstance().getEuid();
            } catch (NativeAccessException exc) {
                // TODO: Added to avoid adding NativeExecutionException to throws clause
                throw new IOException(exc);
            }

            if(uid==0)  // already running as root
                return newLocalChannel();

            String javaExe = System.getProperty("java.home") + "/bin/java";
            File slaveJar = Which.jarFile(Launcher.class);

            ArgumentListBuilder args = new ArgumentListBuilder().add(javaExe);
            if(slaveJar.isFile())
                args.add("-jar").add(slaveJar);
            else // in production code this never happens, but during debugging this is convenientud    
                args.add("-cp").add(slaveJar).add(hudson.remoting.Launcher.class.getName());

            if(rootPassword==null) {
                // try sudo, in the hope that the user has the permission to do so without password
                return new LocalLauncher(listener).launchChannel(
                        args.prepend(sudoExe()).toCommandArray(),
                        listener.getLogger(), null, Collections.<String, String>emptyMap());
            } else {
                // try sudo with the given password. Also run in pfexec so that we can elevate the privileges
                Process proc = sudoWithPass(args);
                return Channels.forProcess(args.toStringWithQuote(), Computer.threadPoolForRemoting, proc,
                        listener.getLogger() );
            }
        }
    }
}
