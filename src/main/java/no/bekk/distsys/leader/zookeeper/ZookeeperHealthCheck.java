package no.bekk.distsys.leader.zookeeper;

import com.codahale.metrics.health.HealthCheck;

public class ZookeeperHealthCheck extends HealthCheck {

    private final ZooKeeperService service;

    public ZookeeperHealthCheck(ZooKeeperService service) {
        this.service = service;
    }


    @Override
    protected Result check() throws Exception {
        boolean alive = service.isAlive();
        if (alive) {
            return Result.healthy();
        }
        return Result.unhealthy("Not OK, state=%s", service.getState());
    }
}
