/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package extensions;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A provider that parses incoming packet extensions into instances of the
 * {@link Class} that it has been instantiated for.
 *
 * @param <C> Class that the packets we will be parsing belong to
 * @author Emil Ivov
 */
public class DefaultPacketExtensionProvider<C extends AbstractPacketExtension> extends ExtensionElementProvider<ExtensionElement> {
    /**
     * The <tt>Logger</tt> used by the <tt>DefaultPacketExtensionProvider</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(DefaultPacketExtensionProvider.class.getName());

    /**
     * The {@link Class} that the packets we will be parsing here belong to.
     */
    private final Class<C> packetClass;

    /**
     * Creates a new packet provider for the specified packet extensions.
     *
     * @param c the {@link Class} that the packets we will be parsing belong to.
     */
    public DefaultPacketExtensionProvider(Class<C> c)
    {
        this.packetClass = c;
    }

    /**
     * Parse an extension sub-packet and create a <tt>C</tt> instance. At
     * the beginning of the method call, the xml parser will be positioned on
     * the opening element of the packet extension and at the end of the method
     * call it will be on the closing element of the packet extension.
     *
     * @param parser an XML parser positioned at the packet's starting element.
     *
     * @return a new packet extension instance.
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    @Override
    public C parse(XmlPullParser parser, int j, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        C packetExtension = null;
        try {
            packetExtension = packetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        //first, set all attributes
        int attrCount = parser.getAttributeCount();

        for (int i = 0; i < attrCount; i++)
        {
            assert packetExtension != null;
            packetExtension.setAttribute(
                            parser.getAttributeName(i),
                            parser.getAttributeValue(i));
        }

        //now parse the sub elements
        boolean done = false;
        XmlPullParser.Event eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (logger.isLoggable(Level.FINEST))
                logger.finest("Will parse " + elementName
                    + " ns=" + namespace
                    + " class=" + packetExtension.getClass().getSimpleName());

            if (eventType == XmlPullParser.Event.START_ELEMENT)
            {
                ExtensionElementProvider<ExtensionElement> provider = ProviderManager
                        .getExtensionProvider( elementName, namespace );

                if(provider == null)
                {
                    //we don't know how to handle this kind of extensions.
                    logger.fine("Could not add a provider for element "
                        + elementName + " from namespace " + namespace);
                }
                else
                {
                    ExtensionElement childExtension
                        = provider.parse(parser);

                    if(namespace != null)
                    {
                        if(childExtension instanceof AbstractPacketExtension)
                        {
                            ((AbstractPacketExtension)childExtension).
                                setNamespace(namespace);
                        }
                    }
                    packetExtension.addChildExtension(childExtension);
                }
            }
            if (eventType == XmlPullParser.Event.END_ELEMENT)
            {
                if (parser.getName().equals(packetExtension.getElementName()))
                {
                    done = true;
                }
            }
            if (eventType == XmlPullParser.Event.TEXT_CHARACTERS)
            {
                String text = parser.getText();
                packetExtension.setText(text);
            }

            if (logger.isLoggable(Level.FINEST))
                logger.finest("Done parsing " + elementName);
        }

        return packetExtension;
    }


}
