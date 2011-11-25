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
package hudson.tasks.mail.impl;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Messages;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Class used for the mail preparation if build was returned to normal state.
 */
public class BackToNormalBuildMail extends BaseBuildResultMail {

    /**
     * Current state.
     */
    private String currentState;

    public BackToNormalBuildMail(String recipients, boolean sendToIndividuals,
                                 List<AbstractProject> upstreamProjects, String charset, String currentState) {
        super(recipients, sendToIndividuals, upstreamProjects, charset);
        this.currentState = currentState;
    }

    /**
     * @inheritDoc
     */
    public MimeMessage getMail(AbstractBuild<?, ?> build, BuildListener listener)
        throws MessagingException, InterruptedException {
        MimeMessage msg = createEmptyMail(build, listener);
        msg.setSubject(getSubject(build, Messages.MailSender_BackToNormalMail_Subject(currentState)), getCharset());
        StringBuilder buf = new StringBuilder();
        appendBuildUrl(build, buf);
        appendFooter(buf);
        msg.setText(buf.toString(), getCharset());
        return msg;
    }
}