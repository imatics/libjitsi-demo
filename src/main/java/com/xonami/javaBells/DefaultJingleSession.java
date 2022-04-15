package com.xonami.javaBells;

import extensions.jingle.JingleIQ;
import extensions.jingle.JinglePacketFactory;
import extensions.jingle.Reason;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import javax.xml.namespace.QName;

/**
 * This is a basic implementation of a JingleSession.
 * Without subclassing this, connections are rejected
 * and most other behavior is a reasonable default.
 *
 * @author bjorn
 *
 */
public class DefaultJingleSession implements JingleSession {
	public enum SessionState {
		NEW,
		NEGOTIATING_TRANSPORT,
		OPEN,
		CLOSED,
	}

	protected final JinglePacketHandler jinglePacketHandler;
	protected final Jid myJid;
	protected final String sessionId;
	protected final XMPPTCPConnection connection;
	protected SessionState state;
	protected Jid peerJid;

	/** creates a new DefaultJingleSession with the given info. */
	public DefaultJingleSession( JinglePacketHandler jinglePacketHandler, String sessionId, XMPPTCPConnection connection ) {
		this.jinglePacketHandler = jinglePacketHandler;
		this.myJid = connection.getUser();
		this.sessionId = sessionId;
		this.connection = connection;
		this.state = SessionState.NEW;
	}

	/** checks to make sure the packet came from the expected peer and that the state is not closed.
	 * If so, it acknowledges and returns true.
	 * If not, it returns false, sends a cancel, and sets the state to closed.
	 * Do NOT call this function before you set the peerJid.
	 */
	protected boolean checkAndAck( JingleIQ jiq ){
		try {
            if( peerJid == null )
                throw new RuntimeException("Don't call this before setting peerJid!");
            if( state == SessionState.CLOSED )
                return false;
            if( peerJid.equals(jiq.getFrom()) ) {
                ack(jiq);
                return true;
            }
            closeSession(Reason.CONNECTIVITY_ERROR);
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

	}

	protected boolean check( JingleIQ jiq ) {
		if( peerJid == null )
			throw new RuntimeException("Don't call this before setting peerJid!");
		if( state == SessionState.CLOSED )
			return false;
		if( peerJid.equals(jiq.getFrom()) ) {
			return true;
		}
        try {
            closeSession(Reason.CONNECTIVITY_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
	}

	/** You may want to override this method to close any Jingle Streams you have open.
	 * To send a close message to the peer, include a reason. If reason is null, no message will be sent.*/
	protected void closeSession(Reason reason) {
        try {
            if( state == SessionState.CLOSED )
                return;
            if( reason != null )
                connection.sendStanza(JinglePacketFactory.createSessionTerminate(myJid, peerJid, sessionId, reason, null));
            state = SessionState.CLOSED;
            jinglePacketHandler.removeJingleSession(this);
        }catch (Exception e){
            e.printStackTrace();
        }

	}

	/** Simply sends an ack to the given iq. */
	public void ack( IQ iq ) {
		IQ resp = IQ.createResultIQ(iq);
        try {
            connection.sendStanza(resp);
        }catch (Exception e){
            e.printStackTrace();
        }
	}
	public void unsupportedInfo( IQ iq ){
		IQ resp = IQ.createResultIQ(iq);
		resp.setType(Type.error);
		resp.addExtension(new ExtensionElement() {
			@Override
			public String getElementName() {
				return "error";
			}
			@Override
			public String getNamespace() {
				return null;
			}

            @Override
            public QName getQName() {
                return ExtensionElement.super.getQName();
            }

            @Override
            public String getLanguage() {
                return ExtensionElement.super.getLanguage();
            }

            @Override
            public CharSequence toXML(XmlEnvironment xmlEnvironment) {
                return null;
            }

            @Override
            public CharSequence toXML(String enclosingNamespace) {
                return ExtensionElement.super.toXML(enclosingNamespace);
            }

            @Override
			public String toXML() {
				return "<error type='modify'><feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/><unsupported-info xmlns='urn:xmpp:jingle:errors:1'/></error>" ;
			}
		});
        try {
            connection.sendStanza(resp);
        }catch (Exception e){
            e.printStackTrace();
        }
	}

	/** Calls checkAndAck. */
	public void handleContentAcept(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleContentAdd(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleContentModify(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleContentReject(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleContentRemove(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleDescriptionInfo(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleSecurityInfo(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleSessionAccept(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleSessionInfo(JingleIQ jiq) {
		checkAndAck(jiq);
	}

	/** sets the peerJid and closes the session. Subclasses will want to
	 * override this if they plan to handle incoming sessions. */
	public void handleSessionInitiate(JingleIQ jiq) {
		if( state == SessionState.CLOSED )
			return;
        try {
            ack(jiq);
		peerJid = jiq.getFrom();
		JingleIQ iq = JinglePacketFactory.createCancel(myJid, peerJid, sessionId);
		connection.sendStanza(iq);
		closeSession(Reason.DECLINE);
        }catch (Exception e){
        e.printStackTrace();
    }
	}

	/** Closes the session. */
	public void handleSessionTerminate(JingleIQ jiq) {
		if( !checkAndAck(jiq) )
			return;
		closeSession(null);
	}
	/** Calls checkAndAck. */
	public void handleTransportAccept(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Calls checkAndAck. */
	public void handleTransportInfo(JingleIQ jiq) {
		checkAndAck(jiq);
	}
	/** Closes the session. */
	public void handleTransportReject(JingleIQ jiq) {
		if( !checkAndAck(jiq) )
			return;
		closeSession(Reason.GENERAL_ERROR);
	}
	/** Calls checkAndAck. */
	public void handleSessionReplace(JingleIQ jiq) {
		checkAndAck(jiq);
	}

	/** returns the sessionId for the current session. */
	@Override
	public String getSessionId() {
		return sessionId;
	}
}
