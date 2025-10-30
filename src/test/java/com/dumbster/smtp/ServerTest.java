/*
 * Dumbster - a dummy SMTP server
 * Copyright 2016 Joachim Nicolay
 * Copyright 2004 Jason Paul Kitchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ServerTest {

    private Server server;

    @BeforeEach
    public void setUp() throws Exception {
        server = Server.start(Server.AUTO_SMTP_PORT);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testSend() throws MessagingException {
        sendMessage(server.getPort(), "sender@here.com", "Test", "Test Body", "receiver@there.com");

        var emails = server.getReceivedEmails();
        assertEquals(1, emails.size());
        var email = emails.getFirst();
        assertEquals("Test", email.getHeaderValue("Subject"));
        assertEquals("Test Body", email.getBody());
        assertContains("Date", email.getHeaderNames());
        assertContains("From", email.getHeaderNames());
        assertContains("To", email.getHeaderNames());
        assertContains("Subject", email.getHeaderNames());
        assertContains("receiver@there.com", email.getHeaderValues("To"));
        assertEquals("receiver@there.com", email.getHeaderValue("To"));
    }

    public static void assertContains(Object expected, Collection<? extends Object> actual) {
        if (actual.contains(expected)) return;
        assertionFailure() //
                .message("%s is not contained in %s".formatted(expected, actual)) //
                .expected(expected) //
                .actual(actual) //
                .buildAndThrow();
    }

    @Test
    public void testSendAndReset() throws MessagingException {
        sendMessage(server.getPort(), "sender@here.com", "Test", "Test Body", "receiver@there.com");
        assertEquals(1, server.getReceivedEmails().size());

        server.reset();
        assertEquals(0, server.getReceivedEmails().size());

        sendMessage(server.getPort(), "sender@here.com", "Test", "Test Body", "receiver@there.com");
        assertEquals(1, server.getReceivedEmails().size());
    }

    @Test
    public void testSendMessageWithCR() throws MessagingException {
        var bodyWithCR = """
                
                
                Keep these pesky carriage returns
                
                
                """;
        sendMessage(server.getPort(), "sender@hereagain.com", "CRTest", bodyWithCR, "receivingagain@there.com");

        var emails = server.getReceivedEmails();
        assertEquals(1, emails.size());
        var email = emails.getFirst();
        assertEquals(bodyWithCR, email.getBody());
    }

    @Test
    public void testSendTwoMessagesSameConnection() throws MessagingException {
        var mimeMessages = new MimeMessage[2];
        var mailProps = getMailProperties(server.getPort());
        var session = Session.getInstance(mailProps, null);

        mimeMessages[0] = createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle1", "Bug1");
        mimeMessages[1] = createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle2", "Bug2");

        var transport = session.getTransport("smtp");
        transport.connect("localhost", server.getPort(), null, null);

        for (MimeMessage mimeMessage : mimeMessages) transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());

        transport.close();

        assertEquals(2, server.getReceivedEmails().size());
    }

    @Test
    public void testSendTwoMsgsWithLogin() throws Exception {
        var serverHost = "localhost";
        var from = "sender@here.com";
        var to = "receiver@there.com";
        var subject = "Test";
        var body = "Test Body";

        var props = System.getProperties();

        props.setProperty("mail.smtp.host", serverHost);

        var session = Session.getDefaultInstance(props, null);
        var msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(from));

        msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject);

        msg.setText(body);
        msg.setHeader("X-Mailer", "musala");
        msg.setSentDate(new Date());
        msg.saveChanges();

        Transport transport = null;

        try {
            transport = session.getTransport("smtp");
            transport.connect(serverHost, server.getPort(), "ddd", "ddd");
            transport.sendMessage(msg, InternetAddress.parse(to, false));
            transport.sendMessage(msg, InternetAddress.parse("dimiter.bakardjiev@musala.com", false));
        } finally {
            if (transport != null) {
                transport.close();
            }
        }

        var emails = this.server.getReceivedEmails();
        assertEquals(2, emails.size());
        var email = emails.getFirst();
        assertEquals("Test", email.getHeaderValue("Subject"));
        assertEquals("Test Body", email.getBody());
    }

    private Properties getMailProperties(int port) {
        var mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", "localhost");
        mailProps.setProperty("mail.smtp.port", "" + port);
        mailProps.setProperty("mail.smtp.sendpartial", "true");
        return mailProps;
    }


    private void sendMessage(int port, String from, String subject, String body, String to) throws MessagingException {
        var mailProps = getMailProperties(port);
        var session = Session.getInstance(mailProps, null);
        //session.setDebug(true);

        var msg = createMessage(session, from, to, subject, body);
        Transport.send(msg);
    }

    private MimeMessage createMessage(
            Session session, String from, String to, String subject, String body) throws MessagingException {
        var msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        msg.setRecipient(RecipientType.TO, new InternetAddress(to));
        return msg;
    }
}
