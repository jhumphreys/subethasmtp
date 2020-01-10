package org.subethamail.smtp.command;

import org.subethamail.smtp.server.SMTPServer;
import org.subethamail.smtp.util.ServerTestCase;
import org.subethamail.smtp.util.Testing;
import org.subethamail.smtp.util.Client;
import org.subethamail.wiser.Wiser;


/**
 * @author Jon Stevens
 */
public class MailTest extends ServerTestCase
{
	private static final int MAX_MESSAGE_SIZE = 1000;

	public MailTest(String name)
	{
		super(name, MAX_MESSAGE_SIZE);
	}

	public void testMailNoHello() throws Exception
	{
		this.expect("220");

		this.send("MAIL FROM: test@example.com");
		this.expect("250");
	}

	public void testAlreadySpecified() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: test@example.com");
		this.expect("250 Ok");

		this.send("MAIL FROM: another@example.com");
		this.expect("503 5.5.1 Sender already specified.");
	}

	public void testInvalidSenders() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");
		
		// added <> because without them "lkjk" is a parameter
		// to the MAIL command. (Postfix responds accordingly)
		this.send("MAIL FROM: <test@lkjsd lkjk>");
		this.expect("553 <test@lkjsd lkjk> Invalid email address.");
	}

	public void testMalformedMailCommand() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL");
		this.expect("501 Syntax: MAIL FROM: <address>  Error in parameters:");
	}

	public void testEmptyFromCommand() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: <>");
		this.expect("250");
	}

	public void testEmptyEmailFromCommand() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM:");
		this.expect("501 Syntax: MAIL FROM: <address>");
	}

	public void testMailWithoutWhitespace() throws Exception
	{
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM:<validuser@subethamail.org>");
		this.expect("250 Ok");
	}

	public void testSize() throws Exception
	{
	    this.expect("220");

	    this.send("EHLO foo.com");
	    this.expectContains("250-SIZE 1000");

	    this.send("MAIL FROM:<validuser@subethamail.org> SIZE=100");
	    this.expect("250 Ok");
	}

	public void testSizeWithoutSize() throws Exception
	{
	    this.expect("220");

	    this.send("EHLO foo.com");
	    this.expectContains("250-SIZE 1000");

	    this.send("MAIL FROM:<validuser@subethamail.org>");
	    this.expect("250 Ok");
	}

	public void testSizeTooLarge() throws Exception
	{
	    this.expect("220");

	    this.send("EHLO foo.com");
	    this.expectContains("250-SIZE 1000");

	    this.send("MAIL FROM:<validuser@subethamail.org> SIZE=1001");
	    this.expect("552");
	}

	public void testUtf8Param() throws Exception
	{
		utf8Server();
		this.expect("220");

		this.send("EHLO foo.com");
	    this.expectContains("250-SMTPUTF8");

	    this.send("MAIL FROM:<valid\u00dcser@subethamail.org> SMTPUTF8");
	    this.expect("250 Ok");
	}

	public void testUtf8ParamUnsupported() throws Exception
	{
		this.expect("220");

		this.send("EHLO foo.com");
		this.expect("250");

	    this.send("MAIL FROM:<validuser@subethamail.org> SMTPUTF8");
	    this.expect("555 5.5.5 SMTPUTF8 extension not supported");
	}

	private void utf8Server() throws Exception {
		this.c.close();
		this.wiser.stop();
		this.wiser = Wiser.accepter(Testing.ACCEPTER).server(SMTPServer
			.port(PORT) 
			.maxMessageSize(MAX_MESSAGE_SIZE)
			.supportUTF8());
		this.wiser.start();
		this.c = new Client("localhost", PORT);
	}

}

