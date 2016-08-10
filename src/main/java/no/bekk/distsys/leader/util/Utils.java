package no.bekk.distsys.leader.util;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class Utils {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Utils.class);
    public static final String APPLICATION_CONNECTOR = "application";
    public static final String ADMIN_CONNECTOR = "admin";


    public static int findPort(Server server, String type) {

        for (final Connector connector : server.getConnectors()) {
            try {
                final ServerSocketChannel channel = (ServerSocketChannel) connector
                        .getTransport();
                final InetSocketAddress socket = (InetSocketAddress) channel
                        .getLocalAddress();

                if (type.equalsIgnoreCase(connector.getName())) {
                    return socket.getPort();
                }
            } catch (Exception e) {
                LOGGER.error(
                        "Unable to get port from connector: "
                                + connector.getName(), e);
            }
        }

        return -1;
    }
}
