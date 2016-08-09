package no.bekk.distsys.leader.dealer;

import com.google.common.eventbus.Subscribe;
import no.bekk.distsys.leader.zookeeper.LeaderElector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/dealer")
public class DealerResource {
    private static final Logger LOG = LoggerFactory.getLogger(DealerResource.class);
    private static final String DEALER_INSTANCE = "/dealers/dealer_";
    private static final String ROOT_DEALERS = "/dealers";

    private AtomicInteger atomicInteger = new AtomicInteger();
    // Start with enabled to false so we dont start dealing numbers without being leader
    private AtomicBoolean isEnabled = new AtomicBoolean(false);

    public DealerResource() {

    }

    public synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            LOG.info("I am now leading!");
        }
        isEnabled.set(enabled);
    }

    @Subscribe
    public void notifyEvent(LeaderElector.LeaderEvent event) {
        switch (event.type) {
            case Leader:
                setEnabled(true);
                break;
            case Follower:
                setEnabled(false);
                break;
        }
    }

    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextNumber() {
        if (!isEnabled.get()) {
            return Response.status(503).entity("{\"error\": \"I am disabled!\"}").build();
        }
        int number = atomicInteger.getAndIncrement();
        String payload = "{\"next\": {Number}}".replace("{Number}", String.valueOf(number));

        return Response.ok(payload).build();
    }

}
