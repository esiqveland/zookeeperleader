package no.bekk.distsys.leader;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LeadingApplicationTest {

    @ClassRule
    public static DropwizardAppRule<LeadingConfiguration> RULE =
            new DropwizardAppRule<>(LeadingApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

    @Test
    public void testStart() {
        assertTrue("App should be up and running, but wasn't. You bruke startup!",
                RULE.getApplication() != null
        );
    }
}
