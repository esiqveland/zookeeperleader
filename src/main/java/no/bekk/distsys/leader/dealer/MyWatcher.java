package no.bekk.distsys.leader.dealer;

import com.google.common.collect.Maps;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.ConcurrentMap;

public class MyWatcher implements Watcher {

    private final DealerResource dealer;
    private ConcurrentMap<String, ZKListener> watchMap = Maps.newConcurrentMap();

    public MyWatcher(DealerResource dealerResource) {
        this.dealer = dealerResource;
    }

    public synchronized void addWatch(String path, ZKListener zkListener) {
        watchMap.putIfAbsent(path, zkListener);
    }
    private void setupWatches(ConcurrentMap<String, ZKListener> watchMap) {

    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getState()) {
            case Disconnected:
                stopDealing();
                break;
            case SyncConnected:
                setupWatches(watchMap);
                break;
            case AuthFailed:
                stopDealing();
                break;
            case ConnectedReadOnly:
                break;
            case SaslAuthenticated:
                break;
            case Expired:
                stopDealing();
                break;
        }
        switch (event.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }
    }


    // TODO: stopDealing
    private void stopDealing() {
        dealer.setEnabled(false);
    }
}
