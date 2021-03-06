/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package extensions.jingle;

import extensions.*;

import static com.xonami.Const.BASE_NAMESPACE;

/**
 * Represents the conference information.
 *
 * @author Sebastien Vincent
 */
public class CoinPacketExtension
    extends AbstractPacketExtension
{
    /**
     * Name of the XML element representing the extension.
     */
    public final static String ELEMENT_NAME = "conference-info";

    /**
     * Namespace.
     */
    public final static String NAMESPACE = BASE_NAMESPACE+"coin";

    /**
     * IsFocus attribute name.
     */
    public final static String ISFOCUS_ATTR_NAME = "isfocus";

    /**
     * Constructs a new <tt>coin</tt> extension.
     *
     */
    public CoinPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Constructs a new <tt>coin</tt> extension.
     *
     * @param isFocus <tt>true</tt> if the peer is a conference focus;
     * otherwise, <tt>false</tt>
     */
    public CoinPacketExtension(boolean isFocus)
    {
        super(NAMESPACE, ELEMENT_NAME);
        setAttribute(ISFOCUS_ATTR_NAME, isFocus);
    }
}
