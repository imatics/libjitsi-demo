package com.xonami.javaBells;

import extensions.jingle.JingleIQ;
import extensions.jingle.JingleIQProvider;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * Manages global smack/jingle features such as converting jingle packets to jingleIQ objects.
 *
 * @author bjorn
 *
 */
public class JingleManager {
	private static boolean enabled = false;

	public static synchronized final void enableJingle() {
		if( enabled )
			return;
		enabled = true;
        ProviderManager.addIQProvider( JingleIQ.ELEMENT_NAME,
                JingleIQ.NAMESPACE,
                new JingleIQProvider());

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public synchronized void connectionCreated(XMPPConnection connection) {
                if( ! ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(JingleIQ.NAMESPACE) )
                    ServiceDiscoveryManager.getInstanceFor(connection).addFeature(JingleIQ.NAMESPACE);
            }

        });
	}
}
