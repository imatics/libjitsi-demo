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
import org.jitsi.utils.MediaType;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.util.List;

/**
 * @author bjorn
 *
 */
public class SampleJinglePacketHandler extends JinglePacketHandler {
	private SampleJingleSession currentJingleSession = null;
	private JingleStreamManager jingleStreamManager = null;
	private IceAgent iceAgent = null;

	public SampleJinglePacketHandler(XMPPTCPConnection connection) {
		super(connection);
	}

	public void initiateOutgoingCall(final Jid targetJid) throws Exception {
		// derive stun and turn server addresses from the connection:
		StunTurnAddress sta = StunTurnAddress.getAddress( connection );
		// create an ice agent using the stun/turn address. We will need this to figure out
		// how to connect our clients:
		iceAgent = new IceAgent(true, sta);
		// setup our jingle stream manager using the default audio and video devices:
		jingleStreamManager = new JingleStreamManager(CreatorEnum.initiator);
		jingleStreamManager.addDefaultMedia(MediaType.VIDEO, "video");
		jingleStreamManager.addDefaultMedia(MediaType.AUDIO, "audio");
		// create ice streams that correspond to the jingle streams that we want
		iceAgent.createStreams(jingleStreamManager.getMediaNames());

		List<ContentPacketExtension> contentList = jingleStreamManager.createContentList(SendersEnum.both);
		iceAgent.addLocalCandidateToContents(contentList);

        JingleIQ sessionInitIQ = JinglePacketFactory.createSessionInitiate(
        		connection.getUser(),
        		targetJid,
        		JingleIQ.generateSID(),
        		contentList );

        connection.sendStanza(sessionInitIQ);
	}

	@Override
	public JingleSession removeJingleSession( JingleSession js ) {
		if( js == currentJingleSession ) {
			currentJingleSession = null;
			jingleStreamManager = null;
			iceAgent = null;
		}
		JingleSession ret = super.removeJingleSession(js);
		return ret;
	}

	@Override
	public JingleSession createJingleSession( String sid, JingleIQ jiq ) {
		if( currentJingleSession != null && currentJingleSession.isActive() )
			return new SampleJingleSession( this, null, null, sid, jiq, connection, SampleJingleSession.CallMode.DONOTANSWER );
		else if( iceAgent != null && jingleStreamManager != null )
			return currentJingleSession = new SampleJingleSession( this, jingleStreamManager, iceAgent, sid, jiq, connection, SampleJingleSession.CallMode.CALL );
		else
			return currentJingleSession = new SampleJingleSession( this, null, null, sid, jiq, connection, SampleJingleSession.CallMode.ANSWER );
	}
}
