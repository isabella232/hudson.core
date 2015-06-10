/*
 * Copyright (c) 2015 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.util;

/**
 *
 * @author Bob Foster
 */
public class TrackedDaemonExecutor extends TrackedExecutor {

    public TrackedDaemonExecutor() {
        factory = new DaemonThreadFactory();
    }

    /**
     * Subclass for threads that are not interruptable.
     */
    public static class NonInterruptableExecutor extends TrackedDaemonExecutor {
        @Override
        public void stopThread(Thread thread) {
            thread.stop();
        }
    }
}
