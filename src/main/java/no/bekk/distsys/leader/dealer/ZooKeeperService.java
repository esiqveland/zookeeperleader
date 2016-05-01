package no.bekk.distsys.leader.dealer;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ZooKeeperService implements Watcher {
    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperService.class);

    private ZooKeeper zooKeeper;
    private final ConcurrentLinkedQueue<ZKListener> listeners = new ConcurrentLinkedQueue<>();
    private final String url;
    private Object ret;

    public ZooKeeperService(final String url) throws IOException {
        this.url = url;
        zooKeeper = new ZooKeeper(url, (int) TimeUnit.SECONDS.toMillis(5), this);

    }

    public void addListener(ZKListener listener) {
        listeners.add(listener);
    }

    public String createNode(final String node, final boolean watch, final boolean ephemereal) {
        String createdNodePath = null;

        try {

            final Stat nodeStat = zooKeeper.exists(node, watch);

            if (nodeStat == null) {
                createdNodePath = zooKeeper.create(node, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, (ephemereal ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.PERSISTENT));
            } else {
                createdNodePath = node;
            }
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return createdNodePath;
    }

    public boolean watchNode(final String node, final boolean watch) {
        boolean watched = false;
        try {
            final Stat nodeStat = zooKeeper.exists(node, watch);

            if (nodeStat != null) {
                watched = true;
            }

        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return watched;
    }

    public List<String> getChildren(final String node, final boolean watch) {
        List<String> childNodes = null;

        try {
            childNodes = zooKeeper.getChildren(node, watch);
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return childNodes;
    }

    @Override
    public void process(WatchedEvent event) {
        LOG.info("[process] event={}", event);
        switch (event.getState()) {
            case Disconnected:
                if (event.getType() == Event.EventType.None) {
                    listeners.forEach(ZKListener::Disconnected);
                }
                break;
            case SyncConnected:
                if (event.getType() == Event.EventType.None) {
                    listeners.forEach(ZKListener::Connected);
                }
                break;
            case AuthFailed:
                listeners.forEach(ZKListener::Disconnected);
                break;
            case ConnectedReadOnly:
                break;
            case SaslAuthenticated:
                break;
            case Expired:
                listeners.forEach(ZKListener::Disconnected);
                // TODO: not sure what to do yet. it's all over...
                System.exit(-1);
                break;
        }
        switch (event.getType()) {
            case None:
                break;
            default:
                listeners.forEach((listener) -> listener.Notify(event));
        }
    }

    /**
     * Caller must do stop() before exiting to make sure we shut down ZK connection properly.
     * @throws Exception
     */
    public void stop() throws Exception {
        zooKeeper.close();
    }

    public boolean isAlive() {
        return zooKeeper.getState().isAlive();
    }

    public ZooKeeper.States getState() {
        return zooKeeper.getState();
    }
}
