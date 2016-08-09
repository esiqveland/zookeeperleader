package no.bekk.distsys.leader.zookeeper;

import org.apache.zookeeper.WatchedEvent;

public interface ZKListener {

    void Disconnected();
    void Connected();
    void Notify(WatchedEvent e);

}
