package no.bekk.distsys.leader.dealer;

import org.apache.zookeeper.WatchedEvent;

public interface ZKListener {

    /** Connected is called after a connection is established. Do setup here! */
    void Connected();

    /** Notify is called when we receive any event from any of our (global) watches. */
    void Notify(WatchedEvent e);

    /** Disconnected is called when the connection is lost and it is all over.
     *  We can't know who is leader (but it is not us!), so do necessary cleanup here. */
    void Disconnected();

}
