package no.bekk.distsys.leader;

import com.google.common.eventbus.EventBus;
import com.smoketurner.dropwizard.consul.ConsulBundle;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.leader.dealer.DealerResource;
import no.bekk.distsys.leader.zookeeper.LeaderElector;
import no.bekk.distsys.leader.zookeeper.ZooKeeperService;
import no.bekk.distsys.leader.zookeeper.ZookeeperHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class LeadingApplication extends Application<LeadingConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(LeadingApplication.class);

    public static void main(String[] args) throws Exception {
        new LeadingApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<LeadingConfiguration> bootstrap) {
        super.initialize(bootstrap);


        bootstrap.addBundle(new ConsulBundle<LeadingConfiguration>(getName()) {
            @Override
            public ConsulFactory getConsulFactory(LeadingConfiguration configuration) {
                return configuration.getConsul();
            }
        });
    }

    @Override
    public void run(LeadingConfiguration config, Environment env) throws Exception {
        LOG.info("Booting in ENVIRONMENT={}", System.getenv("ENVIRONMENT"));

        final EventBus eventBus = new EventBus();

//
//        Consultant consultant = Consultant.builder()
//                .identifyAs("dealer")
//                .setHealthEndpoint(String.format(
//                        "/healthcheck"
//                ))
//                .usingObjectMapper(env.getObjectMapper())
//                .withConsulHost("http://localhost:8500")
//                .validateConfigWith(props -> {
//                    // throw if something is bad.
//                })
//                .onValidConfig(props -> {
//                    LOG.info("new valid config out props={}", props);
//                    eventBus.post(NewConfiguration.create(props));
//                })
//                .build();
//        consultant.registerService(9100);
//
//        env.lifecycle().manage(new Managed() {
//            @Override
//            public void start() throws Exception {
//
//            }
//
//            @Override
//            public void stop() throws Exception {
//                consultant.shutdown();
//            }
//        });

        ZooKeeperService zooKeeperService = new ZooKeeperService(config.getZooKeeper());


        env.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                zooKeeperService.start();
            }

            @Override
            public void stop() throws Exception {
                zooKeeperService.stop();
            }
        });



        LeaderElector leaderElector = new LeaderElector(eventBus, zooKeeperService, "/leaders", "dealer_");
        zooKeeperService.addListener(leaderElector);


        DealerResource dealerResource = new DealerResource();
        eventBus.register(dealerResource);


        env.jersey().register(dealerResource);


        env.healthChecks().register("zookeeper", new ZookeeperHealthCheck(zooKeeperService));
    }

    @Override
    public String getName() {
        return "zookeeperleader-app";
    }


    public static class NewConfiguration {
        public Properties newProps;

        private NewConfiguration(Properties newProps) {
            this.newProps = newProps;
        }

        public static NewConfiguration create(Properties newProps) {
            return new NewConfiguration(newProps);
        }
    }
}
