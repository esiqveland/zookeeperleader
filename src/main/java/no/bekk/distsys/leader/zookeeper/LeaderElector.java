package no.bekk.distsys.leader.zookeeper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javaslang.collection.List;
import javaslang.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Class LeaderElector implements a leader election algorithm under ZK based primitives.
 */
public class LeaderElector implements ZKListener {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final String PREFIX;
    private final String ROOT;
    private final ZooKeeperService zkService;
    private final EventBus bus;

    /** my_node holds our node id as a potential leader */
    private String my_node = "";

    private String watchedLeader = "";

    /**
     * root is the path under which we will watch for leaders.
     * prefix is the prefix for each instance.
     * e.g. if root is "/leaders" and prefix is "dealer_",
     * you will have instances of dealer_123312321 under /leaders:
     * - /leaders/dealer_123312321
     * - /leaders/dealer_321551342
     *
     * The EventBus is for publishing changes in leadership in Events, See @LeaderEvent.
     **/
    public LeaderElector(EventBus bus, ZooKeeperService zkService, String root, String prefix) {
        this.ROOT = root;
        this.zkService = zkService;
        this.PREFIX = prefix;
        this.bus = bus;
        zkService.addListener(this);
    }

    private void registerMe(ZooKeeperService zkService) {
        // make sure root node exists
        Try.of(() -> zkService.createNode(ROOT, false, false))
            .onFailure(throwable -> LOG.debug("Error creating ROOT node={}", ROOT, throwable));

        // register ourselves as a potential leader
        String myPath = zkService.createNode(buildPath(ROOT, PREFIX), false, true);
        setMyId(myPath);
    }

    @Override
    public synchronized void Notify(WatchedEvent e) {
        // Are there any other events where we need to stop being leader?

        switch (e.getType()) {
            case NodeDeleted:
                if (e.getPath().equals(buildPath(ROOT, watchedLeader))) {
                    LOG.info("[Notify] The leader before me has died I must find a new Leader!");
                    // the leader before us has died, start an election
                    runLeaderElection(zkService);
                }
                break;
        }
    }

    /**
     * runLeaderElection does an election.
     * returns true if successful election, false if election must be re-run for some reason.
     */
    private synchronized boolean runLeaderElection(ZooKeeperService zkService) {
        List<String> children = List.ofAll(zkService.getChildren(ROOT, false))
                .sorted();


        // There should be at least one node at this point, as we have already registered ourselves.
        String leader = children.headOption().getOrElse("");
        LOG.info("me={} leader={} children={}", my_node, leader, children);


        boolean isLeader = StringUtils.equals(leader, my_node);


        if (isLeader) {
            bus.post(new LeaderEvent(LeaderEvent.Type.Leader));
            watchedLeader = "";
            return true;
        } else {

            // Register a watch on a node ahead of us, we will never be leader until at least this node is gone
            // and since we are not the leader, someone _must_ be ahead of us here.
            String leaderAheadOfMe = children.get(children.indexOf(my_node) - 1);

            LOG.info("Creating watch on leader ahead of me={}", leaderAheadOfMe);
            String otherLeader = buildPath(ROOT, leaderAheadOfMe);

            if (zkService.exists(otherLeader, true)) {
                LOG.info("Watching leader={}", leaderAheadOfMe);
                watchedLeader = leaderAheadOfMe;
                return true;
            } else {
                // the node was removed while we were electing or our ZK session died, run new election
                return false;
            }
        }
    }


    @Override
    public synchronized void Connected() {
        LOG.info("Connected to ZK!");

        // start as follower until we know
        bus.post(new LeaderEvent(LeaderEvent.Type.Follower));

        registerMe(zkService);

        // try one election, if not, do elections until we have a leader
        if (!runLeaderElection(zkService)) {
            do {
            } while (!runLeaderElection(zkService));
        }
    }

    @Override
    public void Disconnected() {
        LOG.warn("Lost connection, it is over!!");
        stopLeading();
    }

    private void stopLeading() {
        LOG.info("node={} stop leading", my_node);
        setMyId("");
        bus.post(new LeaderEvent(LeaderEvent.Type.Follower));
    }

    public void setMyId(@NotNull String myId) {
        this.my_node = removeRootFromPath(ROOT, myId);
        LOG.info("I am at node={}", this.my_node);
    }

    static String removeRootFromPath(String root, String path) {
        String rootNoSlash = StringUtils.removeEnd(root, "/");
        return StringUtils.removeStart(path, rootNoSlash + "/");
    }

    @VisibleForTesting
    static String buildPath(String root, String node) {
        String rootNoSlash = StringUtils.removeEnd(root, "/");
        return rootNoSlash + "/" + node;
    }

    public static class LeaderEvent {
        public final Type type;

        public LeaderEvent(Type type) {
            this.type = type;
        }

        public enum Type {
            Leader,
            Follower,
        }
    }
}
