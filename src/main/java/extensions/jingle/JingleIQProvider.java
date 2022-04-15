/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package extensions.jingle;

import extensions.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jxmpp.jid.impl.JidCreate;

import java.io.IOException;

/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Emil Ivov
 */
public class JingleIQProvider extends IQProvider<JingleIQ>
{
    /**
     * Creates a new instance of the <tt>JingleIQProvider</tt> and register all
     * jingle related extension providers. It is the responsibility of the
     * application to register the <tt>JingleIQProvider</tt> itself.
     */
    public JingleIQProvider()
    {
//        ProviderManager ProviderManager = ProviderManager.getInstance();

        //<description/> provider
        ProviderManager.addExtensionProvider(
            RtpDescriptionPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <RtpDescriptionPacketExtension>(
                                RtpDescriptionPacketExtension.class));

        //<payload-type/> provider
        ProviderManager.addExtensionProvider(
            PayloadTypePacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <PayloadTypePacketExtension>(
                                PayloadTypePacketExtension.class));

        //<parameter/> provider
        ProviderManager.addExtensionProvider(
            ParameterPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <ParameterPacketExtension>(ParameterPacketExtension.class));

        //<rtp-hdrext/> provider
        ProviderManager.addExtensionProvider(
            RTPHdrExtPacketExtension.ELEMENT_NAME,
            RTPHdrExtPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <RTPHdrExtPacketExtension>(RTPHdrExtPacketExtension.class));

        //<encryption/> provider
        ProviderManager.addExtensionProvider(
            EncryptionPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <EncryptionPacketExtension>(EncryptionPacketExtension.class));

        //<zrtp-hash/> provider
        ProviderManager.addExtensionProvider(
            ZrtpHashPacketExtension.ELEMENT_NAME,
            ZrtpHashPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <ZrtpHashPacketExtension>(ZrtpHashPacketExtension.class));

        //<crypto/> provider
        ProviderManager.addExtensionProvider(
            CryptoPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <CryptoPacketExtension>(CryptoPacketExtension.class));

        //ice-udp transport
        ProviderManager.addExtensionProvider(
            IceUdpTransportPacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<IceUdpTransportPacketExtension>(
                            IceUdpTransportPacketExtension.class));

        //<raw-udp/> provider
        ProviderManager.addExtensionProvider(
            RawUdpTransportPacketExtension.ELEMENT_NAME,
            RawUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<RawUdpTransportPacketExtension>(
                            RawUdpTransportPacketExtension.class));

//        //Google P2P transport
//        ProviderManager.addExtensionProvider(
//            GTalkTransportPacketExtension.ELEMENT_NAME,
//            GTalkTransportPacketExtension.NAMESPACE,
//            new DefaultPacketExtensionProvider<GTalkTransportPacketExtension>(
//                            GTalkTransportPacketExtension.class));

        //ice-udp <candidate/> provider
        ProviderManager.addExtensionProvider(
            CandidatePacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<CandidatePacketExtension>(
                            CandidatePacketExtension.class));

        //raw-udp <candidate/> provider
        ProviderManager.addExtensionProvider(
            CandidatePacketExtension.ELEMENT_NAME,
            RawUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<CandidatePacketExtension>(
                            CandidatePacketExtension.class));

//        //Google P2P <candidate/> provider
//        ProviderManager.addExtensionProvider(
//            GTalkCandidatePacketExtension.ELEMENT_NAME,
//            GTalkTransportPacketExtension.NAMESPACE,
//            new DefaultPacketExtensionProvider<GTalkCandidatePacketExtension>(
//                            GTalkCandidatePacketExtension.class));

        //ice-udp <remote-candidate/> provider
        ProviderManager.addExtensionProvider(
            RemoteCandidatePacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<RemoteCandidatePacketExtension>(
                            RemoteCandidatePacketExtension.class));

        //inputevt <inputevt/> provider
        ProviderManager.addExtensionProvider(
                InputEvtPacketExtension.ELEMENT_NAME,
                InputEvtPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                InputEvtPacketExtension.class));

        //coin <conference-info/> provider
        ProviderManager.addExtensionProvider(
                CoinPacketExtension.ELEMENT_NAME,
                CoinPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                CoinPacketExtension.class));

        /*
         * XEP-0251: Jingle Session Transfer <transfer/> and <transferred>
         * providers
         */
        ProviderManager.addExtensionProvider(
                TransferPacketExtension.ELEMENT_NAME,
                TransferPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferPacketExtension>(
                        TransferPacketExtension.class));
        ProviderManager.addExtensionProvider(
                TransferredPacketExtension.ELEMENT_NAME,
                TransferredPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferredPacketExtension>(
                        TransferredPacketExtension.class));
    }

//    @Override
//    public JingleIQ parse(XmlPullParser xmlPullParser, int i, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
//        return null;
//    }

    /**
     * Parses a Jingle IQ sub-document and returns a {@link JingleIQ} instance.
     *
     * @param parser an XML parser.
     *
     * @return a new {@link JingleIQ} instance.
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    public JingleIQ parse(XmlPullParser parser, int i, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException
    {
        JingleIQ jingleIQ = new JingleIQ();

        //let's first handle the "jingle" element params.
        JingleAction action = JingleAction.parseString(parser
                        .getAttributeValue("", JingleIQ.ACTION_ATTR_NAME));
        String initiator = parser
                         .getAttributeValue("", JingleIQ.INITIATOR_ATTR_NAME);
        String responder = parser
                        .getAttributeValue("", JingleIQ.RESPONDER_ATTR_NAME);
        String sid = parser
                        .getAttributeValue("", JingleIQ.SID_ATTR_NAME);

        jingleIQ.setAction(action);
        jingleIQ.setInitiator(JidCreate.bareFrom(initiator));
        jingleIQ.setResponder(JidCreate.bareFrom(responder));
        jingleIQ.setSID(sid);

        boolean done = false;

        // Sub-elements providers
        DefaultPacketExtensionProvider<ContentPacketExtension> contentProvider
            = new DefaultPacketExtensionProvider<ContentPacketExtension>(
                    ContentPacketExtension.class);
        ReasonProvider reasonProvider = new ReasonProvider();
        DefaultPacketExtensionProvider<TransferPacketExtension> transferProvider
            = new DefaultPacketExtensionProvider<TransferPacketExtension>(
                    TransferPacketExtension.class);
        DefaultPacketExtensionProvider<CoinPacketExtension> coinProvider
            = new DefaultPacketExtensionProvider<CoinPacketExtension>(
                    CoinPacketExtension.class);

        // Now go on and parse the jingle element's content.
        XmlPullParser.Event eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.Event.START_ELEMENT)
            {
                // <content/>
                if (elementName.equals(ContentPacketExtension.ELEMENT_NAME))
                {
                    ContentPacketExtension content
                        = contentProvider.parse(parser, i, xmlEnvironment);
                    jingleIQ.addContent(content);
                }
                // <reason/>
                else if(elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parse(parser);
                    jingleIQ.setReason(reason);
                }
                // <transfer/>
                else if (elementName.equals(
                                TransferPacketExtension.ELEMENT_NAME)
                        && namespace.equals(TransferPacketExtension.NAMESPACE))
                {
                    jingleIQ.addExtension(
                            transferProvider.parse(parser));
                }
                else if(elementName.equals(CoinPacketExtension.ELEMENT_NAME))
                {
                    jingleIQ.addExtension(coinProvider.parse(parser));
                }

                //<mute/> <active/> and other session-info elements
                if (namespace.equals( SessionInfoPacketExtension.NAMESPACE))
                {
                    SessionInfoType type = SessionInfoType.valueOf(elementName);

                    //<mute/>
                    if( type == SessionInfoType.mute
                        || type == SessionInfoType.unmute)
                    {
                        String name = parser.getAttributeValue("",
                                MuteSessionInfoPacketExtension.NAME_ATTR_VALUE);

                        jingleIQ.setSessionInfo(
                                new MuteSessionInfoPacketExtension(
                                        type == SessionInfoType.mute, name));
                    }
                    //<hold/>, <unhold/>, <active/>, etc.
                    else
                    {
                        jingleIQ.setSessionInfo(
                                        new SessionInfoPacketExtension(type));
                    }
                }
            }

            if ((eventType == XmlPullParser.Event.END_ELEMENT)
                    && parser.getName().equals(JingleIQ.ELEMENT_NAME))
            {
                    done = true;
            }
        }
        return jingleIQ;
    }
}
