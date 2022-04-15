/**
 *
 */
package com.xonami.javaBellsSample;

import com.xonami.javaBells.*;
import extensions.jingle.ContentPacketExtension;
import extensions.jingle.ContentPacketExtension.CreatorEnum;
import extensions.jingle.ContentPacketExtension.SendersEnum;
import extensions.jingle.JingleIQ;
import extensions.jingle.JinglePacketFactory;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.utils.MediaType;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * This is the main class for demonstrating jingle bells.
 * Compile and run without arguments for usage.
 *
 * @author bjorn
 *
 */
public class JavaBellsSample {
	enum Action {
		CALL,
		ANSWER,
		CALL_AND_ANSWER
	}
	protected final static Logger logger = LoggerFactory.getLogger(Logger.class);

	private static final String CALLER = "Caller";
	private static final String RECEIVER = "Receiver";

	private final String username, password, host;

	private final Jid callerJid, receiverJid;

	private boolean running = true;
	private Thread answerThread, callThread;

	/** prints usage and exits. */
	public static void usage(String name) {
		System.out.println( "Usage: " + name + " action username password host" );
		System.out.println( "\t action: may be CALL, ANSWER or CALL_AND_ANSWER" );
		System.out.println( "\t best to run twice with CALL and ANSWER than CALL_AND_ANSWER" );
		System.out.println( "\t because there is a race condition with CALL_AND_ANSWER." );
		System.exit(1);
	}

	public static void main(String[] args) throws XmppStringprepException {
		if( args.length != 5 )
			usage(args[0]);

		Thread.setDefaultUncaughtExceptionHandler( new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error("In thread: ", t);
				logger.error("Uncaught Exception: ", e);
			}
		}) ;

		// reduce the insane, unreadable amount of chattiness from libjitsi and ice4j:
		java.util.logging.Logger l = java.util.logging.Logger.getLogger("");
		l.setLevel(Level.WARNING);

		// -- libjitsi needs to be started
		LibJitsi.start();

		// -- we need to initialize jingle
		JingleManager.enableJingle();

		// parse commandline args:
		String cmd      = args[0];
		System.out.println( "cmd: " + cmd );
		Action action   = Action.valueOf(args[1]);
		System.out.println( "action: " + action );
		String username = args[2];
		String password = args[3];
		String host     = args[4];

		System.out.println( "u/p @ h: " + username + " / " + password + " @ " + host);

		// start threads to actually do the work of calling/answering:
		JavaBellsSample m = new JavaBellsSample( username, password, host );
		if( action == Action.ANSWER || action == Action.CALL_AND_ANSWER ) {
			m.startAnswer();
		}
		if( action == Action.CALL || action == Action.CALL_AND_ANSWER ) {
			m.startCall();
		}

		// stop threads when the user hits enter, cleanup and exit.
		System.out.println( "Hit enter to stop: " );
		while( true )
			try {
				System.in.read();
				break;
			} catch (IOException e) {}
		m.joinAll();
		LibJitsi.stop();
		System.exit(0);
	}

	/** creates a new object with the given username and password on the given host. */
	public JavaBellsSample( String username, String password, String host ) throws XmppStringprepException {
		this.username = username;
		this.password = password;
		this.host     = host;
		callerJid = JidCreate.bareFrom(username + "@" + host + "/" + CALLER) ;
		receiverJid = JidCreate.bareFrom(username + "@" + host + "/" + RECEIVER );
	}

	/** waits for both calling and answering thread to return. */
	public void joinAll() {
		running = false;
		if( answerThread != null )
			while( true )
				try {
					synchronized( answerThread ) {
						answerThread.notify();
					}
					answerThread.join();
					break;
				} catch (InterruptedException e) {}
		if( callThread != null )
			while( true )
				try {
					synchronized( callThread ) {
						callThread.notify();
					}
					callThread.join();
					break;
				} catch (InterruptedException e) {}
	}

	/** starts a "receiver" in another thread. The receiver connects to the XMPP server,
	 * and waits for a jingle session initiation request from the caller.
	 */
	public void startAnswer() {
		answerThread = new Thread() {
			@Override
			public void run() {
				try {
					log( RECEIVER, "connecting to " + host );

					// connect to host (don't log in yet)
					XMPPTCPConnectionConfiguration.Builder config =  XMPPTCPConnectionConfiguration.builder();
                    config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
                    config.setHost(host);
					XMPPTCPConnection connection = new XMPPTCPConnection( config.build() );
					connection.connect();
					// setup service discovery and entity capabilities.
					// this ensures that other software, such as Jitsi, knows that we support
					// ice and so on
					//ServiceDiscoveryManager.setIdentityName("Java Bells");
					ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection);
					EntityCapsManager ecm = EntityCapsManager.getInstanceFor(connection);

					ecm.enableEntityCaps();

					disco.addFeature("http://jabber.org/protocol/disco#info");
					disco.addFeature("urn:xmpp:jingle:1");
					disco.addFeature("urn:xmpp:jingle:transports:ice-udp:1");
					disco.addFeature("urn:xmpp:jingle:apps:rtp:1");
					disco.addFeature("urn:xmpp:jingle:apps:rtp:audio");
					disco.addFeature("urn:xmpp:jingle:apps:rtp:video");

					// Handle all incoming Jingle packets with a Jingle Packet Handler.
					// The main thing we need to do is ensure that created Jingle sessions
					// are of our ReceiverJingleSession type.
					new JinglePacketHandler(connection) {
						@Override
						public JingleSession createJingleSession( String sid, JingleIQ jiq ) {
							return new ReceiverJingleSession(this, callerJid, sid, this.connection );
						}
					} ;

					// display out all packets that get sent:
                    connection.addStanzaSendingListener(new StanzaListener() {
                        @Override
                        public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
                            System.out.println( RECEIVER + " SENDING : " + stanza.toXML());
                        }
                    }, stanza -> true);

					// display incoming jingle packets
                    connection.addStanzaListener(new StanzaListener() {
                        @Override
                        public void processStanza(Stanza stanza) {
                            JingleIQ j = (JingleIQ) stanza;
                            System.out.println( RECEIVER + "[jingle packet]: " + j.getSID() + " : " + j.getAction() );
                        }
                    }, stanza ->  stanza.getClass() == JingleIQ.class);

					//display all incoming packets
                    connection.addStanzaListener(new StanzaListener() {
                        @Override
                        public void processStanza(Stanza stanza) {
                            System.out.println( RECEIVER + ": " + stanza.toXML());
                        }
                    }, stanza -> true);


					// -- log in
					log( RECEIVER, "logging on as " + username + "/" + RECEIVER );
					connection.login(username, password, Resourcepart.from(RECEIVER));

					// -- just hang out until we are asked to exit.
					// the work will be done by the ReceiverJingleSession
					// we created and applied earlier.
					log( RECEIVER, "Waiting..." );
					while( running ) {
						synchronized ( this ) {
							try {
								wait(1000);
							} catch (InterruptedException e) {}
						}
					}
					connection.disconnect();
					log( RECEIVER, "Done. Exiting thread." );
				} catch ( Exception e ) {
					System.out.println( RECEIVER + ": " + e );
					e.printStackTrace();
					System.exit(1);
				}
			}
		};
		answerThread.start();
	}

	/** starts a "caller" in another thread. The caller connects to the XMPP server,
	 *  sends the "receiver" a jingle request.
	 */
	public void startCall() {
		callThread = new Thread() {
			@Override
			public void run() {
				try {
					log( CALLER, "connecting to " + host );

					// connect to the XMPP server. Don't log in yet.
                    XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
                    config.setHost(host);
                    config.setXmppDomain(host);
                    config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
					XMPPTCPConnection connection = new XMPPTCPConnection( config.build() );
					connection.connect();

					// derive stun and turn server addresses from the connection:
					StunTurnAddress sta = StunTurnAddress.getAddress( connection );
					// create an ice agent using the stun/turn address. We will need this to figure out
					// how to connect our clients:
					final IceAgent iceAgent = new IceAgent(true, sta);
					// setup our jingle stream manager using the default audio and video devices:
					final JingleStreamManager jsm = new JingleStreamManager(CreatorEnum.initiator);
					jsm.addDefaultMedia(MediaType.VIDEO, "video");
					jsm.addDefaultMedia(MediaType.AUDIO, "audio");
					// create ice streams that correspond to the jingle streams that we want
					iceAgent.createStreams(jsm.getMediaNames());

					// Handle all incoming Jingle packets with a Jingle Packet Handler.
					// The main thing we need to do is ensure that created Jingle sessions
					// are of our ReceiverJingleSession type.
					new JinglePacketHandler(connection) {
						@Override
						public JingleSession createJingleSession( String sid, JingleIQ jiq ) {
							return new CallerJingleSession(iceAgent, jsm, this, receiverJid, sid, this.connection);
						}
					} ;

					// display all incoming packets:
                    connection.addStanzaListener(stanza -> System.out.println( CALLER + ": " + stanza.toXML() ), stanza -> stanza.getClass() == JingleIQ.class);


//					PacketCollector collector = connection.createPacketCollector( new PacketFilter() {
//						@Override
//						public boolean accept(Packet packet) {
//							return true;
//						}} );

					// log in:
					log( CALLER, "logging on as " + username + "/" + CALLER );
					connection.login(username, password, Resourcepart.from(CALLER));

					//this only works if they are in our roster
//					log( CALLER, "Waiting for Receiver to become available." );
//					while( running && !connection.getRoster().contains(receiverJid) ) {
//						collector.nextResult(100);
//					}

					// Use Ice and JingleSessionManager to initiate session:
					log( CALLER, "Ringing" );
					List<ContentPacketExtension> contentList = jsm.createContentList(SendersEnum.both);
					iceAgent.addLocalCandidateToContents(contentList);

		            JingleIQ sessionInitIQ = JinglePacketFactory.createSessionInitiate(
		            		connection.getUser(),
		            		JidCreate.bareFrom(receiverJid),
		            		JingleIQ.generateSID(),
		            		contentList );

		            System.out.println( "CALLER: sending jingle request: " + sessionInitIQ.toXML() );

		            connection.sendStanza(sessionInitIQ);

		            // now hang out until the user requests that we exit.
		            // The rest of the call will be handled by the CallerJingleSession we created above.
					log( CALLER, "Waiting..." );
					while( running ) {
						synchronized ( this ) {
							try {
								wait(1000);
							} catch (InterruptedException e) {}
						}
					}
					connection.disconnect();
					log( CALLER, "Done. Exiting thread." );
				} catch ( Exception e ) {
					System.out.println( CALLER + ": " + e );
					e.printStackTrace();
					System.exit(1);
				}
			}
		};
		callThread.start();
	}


	private void log( String tag, String message ) {
		logger.info( "[{}]: {}", tag, message );
	}
}
