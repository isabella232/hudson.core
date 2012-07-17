/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Anton Kozak
 *
 *******************************************************************************/
package hudson.tasks.mail;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Interface describes mail with project build result.
 */
public interface BuildResultMail {

    /**
     * Returns prepared mail.
     *
     * @param build build.
     * @param listener listener.
     *
     * @return prepared mail.
     * @throws MessagingException exception if any.
     * @throws InterruptedException exception if any.
     */
    MimeMessage getMail(AbstractBuild<?, ?> build, BuildListener listener)
            throws MessagingException, InterruptedException;
}