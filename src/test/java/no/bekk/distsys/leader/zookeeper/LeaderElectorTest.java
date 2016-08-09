package no.bekk.distsys.leader.zookeeper;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;

public class LeaderElectorTest {

    @Test
    public void testLeaderSorting() {
        ArrayList<String> children = Lists.newArrayList(
                "dealer_0000003205",
                "dealer_0000003203",
                "dealer_0000003206"
        );
        Collections.sort(children);

        assertThat(children.get(0)).isEqualTo("dealer_0000003203");
    }

}
