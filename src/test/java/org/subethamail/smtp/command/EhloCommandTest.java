package org.subethamail.smtp.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.Before;

import org.mockito.Mockito;
import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.internal.command.EhloCommand;
import org.subethamail.smtp.internal.server.ServerThread;
import org.subethamail.smtp.server.SMTPServer;
import org.subethamail.smtp.server.Session;

public class EhloCommandTest {

    private SMTPServer.Builder serverBuilder;

    @Before
    public void setUpSMTPServer() {
        serverBuilder = SMTPServer.port(2525);
    }

    @Test
    public void testEhloWhenTlsRequiredAuthShouldNotAdvertiseBeforeTlsStarted() throws IOException {
        serverBuilder.enableTLS().requireTLS();
        enableAuth();
        String output = getOutput(false);
        System.out.println(output);
        assertTrue(output.contains("250-STARTTLS"));
        assertFalse(output.contains("250-AUTH PLAIN"));
    }

    @Test
    public void testEhloWhenTlsRequiredAuthShouldBeAdvertiseAfterTlsStarted() throws IOException {
        serverBuilder.enableTLS().requireTLS();
        enableAuth();
        String output = getOutput(true);
        System.out.println(output);
        assertTrue(output.contains("250-STARTTLS"));
        assertTrue(output.contains("250-AUTH PLAIN"));
    }

    @Test
    public void testEhloWhenUTF8Supported () throws Exception {
        serverBuilder.supportUTF8(true);
        String output = getOutput(true);
        assertTrue(output.contains("250-SMTPUTF8"));
    }

    @Test
    public void testEhloWhenUTF8NotSupported () throws Exception {
        serverBuilder.supportUTF8(false);
        String output = getOutput(true);
        assertFalse(output.contains("250-SMTPUTF8"));
    }


    private void enableAuth() {
        serverBuilder.authenticationHandlerFactory(new AuthenticationHandlerFactory() {
                @Override
                public List<String> getAuthenticationMechanisms() {
                    return Collections.singletonList("PLAIN");
                }

                @Override
                public AuthenticationHandler create() {
                    return null;
                }
            });

    }

    private String getOutput(boolean isTlsStarted) throws IOException {
        EhloCommand ec = new EhloCommand();
        try (ServerSocket ss = new ServerSocket(0)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Socket socket = Mockito.mock(Socket.class);
            Mockito.when(socket.getOutputStream()).thenReturn(out);
            SMTPServer server = serverBuilder
                .port(ss.getLocalPort())
                .serverSocketFactory(() -> ss)
                .build();
            Session session = new Session(server, new ServerThread(server, ss), socket);
            session.setTlsStarted(isTlsStarted);
            ec.execute("EHLO me.com", session);
            String output = new String(out.toByteArray(), StandardCharsets.UTF_8);
            return output;
        }
    }

}
