package no.bekk.distsys.leader.zookeeper;

import com.google.common.collect.Lists;
import javaslang.collection.List;
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

    @Test
    public void testLeaderSorting_3() {
        List<String> children = List.of(
                "dealer_0000003203",
                "dealer_0000003205",
                "dealer_0000003206",
                "dealer_0000003207"
        );

        assertThat(children.indexOf("dealer_0000003205")).isEqualTo(1);

    }
    @Test
    public void testLeaderSorting_2() {
        List<String> expected = List.of(
                "dealer_0000003203",
                "dealer_0000003205",
                "dealer_0000003206",
                "dealer_0000003207"
        );

        List<String> sorted = List.of(
                "dealer_0000003207",
                "dealer_0000003205",
                "dealer_0000003203",
                "dealer_0000003206"
        ).sorted();


        assertThat(sorted.get(0)).isEqualTo("dealer_0000003203");
        assertThat(sorted).isEqualTo(expected);
    }

}
