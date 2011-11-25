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
package hudson.mail;

import hudson.tasks.HudsonMimeMessage;
import hudson.tasks.Mailer;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Base logic of sending out notification e-mail.
 */
public abstract class BaseMailSender {
    private static final String DEFAULT_TEXT = "Should be overridden";

    protected static final String DEFAULT_CHARSET = "UTF-8";
    /**
     * Whitespace-separated list of e-mail addresses that represent recipients.
     */
    private String recipients;

    /**
     * The charset to use for the text and subject.
     */
    private String charset;


    public BaseMailSender(String recipients) {
        this(recipients, DEFAULT_CHARSET);
    }

    public BaseMailSender(String recipients, String charset) {
        this.recipients = recipients;
        this.charset = charset;
    }

    /**
     * Returns recipients.
     *
     * @return recipients.
     */
    public String getRecipients() {
        return recipients;
    }

    /**
     * Returns charset.
     *
     * @return charset.
     */
    public String getCharset() {
        return charset;
    }

    public boolean execute() {
        try {
            MimeMessage mail = getMail();
            if (mail != null) {
                if (mail.getAllRecipients() != null) {
                    Mailer.descriptor().send((HudsonMimeMessage) mail);
                }
            }
        } catch (MessagingException ignore) {
            //TODO add logging
        }
        return true;
    }

    /**
     * Returns prepared email.
     *
     * @return prepared email.
     * @throws MessagingException exception if any.
     */
    protected MimeMessage getMail() throws MessagingException {
        MimeMessage msg = new HudsonMimeMessage(Mailer.descriptor().createSession());
        msg.setContent("", "text/plain");
        msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
        msg.setSentDate(new Date());

        Set<InternetAddress> rcp = new LinkedHashSet<InternetAddress>();
        StringTokenizer tokens = new StringTokenizer(recipients);
        while (tokens.hasMoreTokens()) {
            String address = tokens.nextToken();
            try {
                rcp.add(new InternetAddress(address));
            } catch (AddressException ignore) {
                // ignore bad address, but try to send to other addresses
            }
        }
        msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));
        msg.setSubject(new StringBuilder().append(getSubjectPrefix()).append(" ").append(getSubject()).toString(),
            charset);
        msg.setText(new StringBuilder().append(getText()).append(getTextFooter()).toString(), charset);
        return msg;
    }

    /**
     * Returns the text of the email.
     *
     * @return the text of the email.
     */
    protected String getText() {
        return DEFAULT_TEXT;
    }

    /**
     * Returns the subject of the email.
     *
     * @return the subject of the email.
     */
    protected String getSubject() {
        return DEFAULT_TEXT;
    }

    /**
     * Returns text footer for all automatically generated emails.
     *
     * @return text footer.
     */
    protected String getTextFooter() {
        return hudson.mail.Messages.hudson_email_footer();
    }

    /**
     * Returns prefix for subject of automatically generated emails.
     *
     * @return prefix for subject.
     */
    protected String getSubjectPrefix() {
        return hudson.mail.Messages.hudson_email_subject_prefix();
    }

}
