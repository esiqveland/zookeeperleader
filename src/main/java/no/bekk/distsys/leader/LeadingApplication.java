package no.bekk.distsys.leader;

import com.google.common.eventbus.EventBus;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.leader.dealer.DealerResource;
import no.bekk.distsys.leader.zookeeper.LeaderElector;
import no.bekk.distsys.leader.zookeeper.ZooKeeperService;
import no.bekk.distsys.leader.zookeeper.ZookeeperHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeadingApplication extends Application<LeadingConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(LeadingApplication.class);

    public static void main(String[] args) throws Exception {
        new LeadingApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<LeadingConfiguration> bootstrap) {
        super.initialize(bootstrap);
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
        ));
    }

    @Override
    public void run(LeadingConfiguration config, Environment env) throws Exception {
        LOG.info("Booting in ENVIRONMENT={}", System.getenv("ENVIRONMENT"));


        ZooKeeperService zooKeeperService = new ZooKeeperService(config.getZooKeeper());


        /** We register this because of a DW hook to shutdown service on JVM exit.
         *  This closes the ZK connection (kills the session)
         *  so we don't have to wait 2 timeouts before the cluster can remove our session */
        env.lifecycle().manage(zooKeeperService);


        DealerResource dealerResource = new DealerResource();

        // We use the eventbus to publish election events
        EventBus eventBus = new EventBus();
        eventBus.register(dealerResource);


        LeaderElector leaderElector = new LeaderElector(eventBus, zooKeeperService, "/leaders", "dealer_");


        env.jersey().register(dealerResource);


        env.healthChecks().register("zookeeper", new ZookeeperHealthCheck(zooKeeperService));
    }

    @Override
    public String getName() {
        return "zookeeperleader-app";
    }

}
