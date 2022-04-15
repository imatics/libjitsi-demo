/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package extensions.jingle;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;

/**
 * The <tt>ReasonProvider</tt> parses "reason" elements into {@link
 * ReasonPacketExtension} instances.
 *
 * @author Emil Ivov
 */
public class ReasonProvider extends ExtensionElementProvider<ReasonPacketExtension> {

    /**
     * Parses a reason extension sub-packet and creates a {@link
     * ReasonPacketExtension} instance. At the beginning of the method call,
     * the xml parser will be positioned on the opening element of the packet
     * extension. As required by the smack API, at the end of the method call,
     * the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening <tt>reason</tt>
     * element.
     *
     * @return a new {@link ReasonPacketExtension} instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    @Override
    public ReasonPacketExtension parse(XmlPullParser parser, int i, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException
    {
        String text = null;
        Reason reason = null;

        boolean done = false;

        XmlPullParser.Event eventType;
        String elementName;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.Event.START_ELEMENT)
            {
                // the reason itself.
                if( reason == null)
                {
                    //let the parse exception fly as it would mean we have
                    //some weird element first in the list.
                    reason = Reason.parseString(elementName);
                }
                else if (elementName.equals(
                                ReasonPacketExtension.TEXT_ELEMENT_NAME))
                {
                    try {
                        text = parseText(parser);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    //this is an element that we don't currently support.
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT)
            {
                if (parser.getName().equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }
        ReasonPacketExtension reasonExt
            = new ReasonPacketExtension(reason, text, null);

        return reasonExt;

    }

    /**
     * Returns the content of the next {@link XmlPullParser#TEXT} element that
     * we encounter in <tt>parser</tt>.
     *
     * @param parser the parse that we'll be probing for text.
     *
     * @return the content of the next {@link XmlPullParser#TEXT} element we
     * come across or <tt>null</tt> if we encounter a closing tag first.
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    public String parseText(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;

        XmlPullParser.Event eventType;
        String text = null;

        while (!done)
        {
            eventType = parser.next();

            if (eventType == XmlPullParser.Event.TEXT_CHARACTERS)
            {
                text = parser.getText();
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT)
            {
                done = true;
            }
        }

        return text;
    }


}
