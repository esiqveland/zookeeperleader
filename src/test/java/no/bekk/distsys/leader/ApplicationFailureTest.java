package no.bekk.distsys.leader;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.tomakehurst.crashlab.CrashLab;
import com.tomakehurst.crashlab.HttpSteps;
import com.tomakehurst.crashlab.TimeInterval;
import com.tomakehurst.crashlab.metrics.AppMetrics;
import com.tomakehurst.crashlab.metrics.HttpJsonAppMetricsSource;
import com.tomakehurst.crashlab.saboteur.Saboteur;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.tomakehurst.crashlab.Rate.rate;
import static com.tomakehurst.crashlab.TimeInterval.interval;
import static com.tomakehurst.crashlab.TimeInterval.period;
import static com.tomakehurst.crashlab.saboteur.Delay.delay;
import static com.tomakehurst.crashlab.saboteur.FirewallTimeout.firewallTimeout;
import static com.tomakehurst.crashlab.saboteur.PacketLoss.packetLoss;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class ApplicationFailureTest {

    @ClassRule
    public static DropwizardAppRule<LeadingConfiguration> RULE =
            new DropwizardAppRule<>(LeadingApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

//    @ClassRule
//    public static WireMockClassRule wireMockRule = new WireMockClassRule(new WireMockConfiguration().port(8080));
//    @Rule
//    public WireMockClassRule instanceRule = wireMockRule;

    public WireMock wireMock = new WireMock("192.168.2.2", 8080);
    private Saboteur saboteur = Saboteur.defineClient("mock-web-service", 8080, "192.168.2.2");

    /** private non-static to access portnumber from RULE */
    private HttpJsonAppMetricsSource metricsSource = new HttpJsonAppMetricsSource(
            String.format("http://localhost:%d/metrics", RULE.getAdminPort())
    );
    static CrashLab crashLab = new CrashLab();

    @Before
    public void init() {
        saboteur.reset();
//        wireMock.resetMappings();
    }

    @Test(timeout = 25000)
    public void latency_less_than_300ms_with_no_faults() {
        //instanceRule.stubFor(responseOk());
        wireMock.register(responseOk());

        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        crashLab.run(period(10, SECONDS), rate(500).per(SECONDS), new HttpSteps("10 seconds moderate load") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });

        AppMetrics appMetrics = metricsSource.fetch();
        TimeInterval p95 = appMetrics.timer("no.bekk.distsys.leader.pinger.PingResource.getAnswer").percentile95();
        assertTrue("Expected 95th percentile latency to be less than 300 milliseconds. Was actually " + p95.timeIn(MILLISECONDS) + "ms",
                p95.lessThan(interval(300, MILLISECONDS)));
    }

    @Test(timeout = 25000)
    public void latency_less_than_200ms_with_no_faults_during_network_failure() {
        //instanceRule.stubFor(responseOk());
        wireMock.register(responseOk());

        saboteur.addFault(packetLoss("1 percent").probability(40).correlation(15).setToPort(8080));

        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        crashLab.run(period(10, SECONDS), rate(200).per(SECONDS), new HttpSteps("10 seconds moderate load") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });
        AppMetrics appMetrics = metricsSource.fetch();
        TimeInterval p95 = appMetrics.timer("no.bekk.distsys.leader.pinger.PingResource.getAnswer").percentile95();
        assertTrue("Expected 95th percentile latency to be less than 300 milliseconds. Was actually " + p95.timeIn(MILLISECONDS) + "ms",
                p95.lessThan(interval(300, MILLISECONDS)));
    }

    @Test(timeout = 25000)
    public void latency_less_than_300ms_with_during_network_failure() {
        //instanceRule.stubFor(responseOk());
        wireMock.register(responseOk());

        saboteur.addFault(packetLoss("all traffic").probability(100.0).setToPort(8080));

        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        crashLab.run(period(10, SECONDS), rate(300).per(SECONDS), new HttpSteps("10 seconds high load") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });
        AppMetrics appMetrics = metricsSource.fetch();
        TimeInterval p95 = appMetrics.timer("no.bekk.distsys.leader.pinger.PingResource.getAnswer").percentile95();
        assertTrue("Expected 95th percentile latency to be less than 300 milliseconds. Was actually " + p95.timeIn(MILLISECONDS) + "ms",
                p95.lessThan(interval(300, MILLISECONDS)));
    }

    @Test(timeout = 25000)
    public void latency_less_than_200ms_with_no_faults_during_network_delays() {
        //instanceRule.stubFor(responseOk());
        wireMock.register(responseOk());

        saboteur.addFault(delay("network-delay").delay(100, MILLISECONDS).variance(70, MILLISECONDS).setToPort(8080));

        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        crashLab.run(period(10, SECONDS), rate(500).per(SECONDS), new HttpSteps("10 seconds moderate load") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });
           AppMetrics appMetrics = metricsSource.fetch();
        TimeInterval p95 = appMetrics.timer("no.bekk.distsys.leader.pinger.PingResource.getAnswer").percentile95();
        assertTrue("Expected 95th percentile latency to be less than 300 milliseconds. Was actually " + p95.timeIn(MILLISECONDS) + "ms",
                p95.lessThan(interval(300, MILLISECONDS)));
    }

    @Test(timeout = 25000)
    public void test_connection_pool_does_not_lock_after_network_glitches() throws Exception {
        wireMock.register(responseOk());

        saboteur.addFault(firewallTimeout("firewall-delay").timeout(5, SECONDS).setToPort(8080));
        runWithRate(5, 200, SECONDS, "Run with falling firewall");

        sleepUninterruptibly(6, SECONDS);

        // Do one request to see all our threads have not been eaten.
        AsyncHttpClient httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs(1000).build());
        final String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());
        Response response = httpClient.prepareGet(GET_ANSWER).execute().get();
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getResponseBody(), is(EXPECTED));
    }

    @Test(timeout = 20000)
    public void test_connection_pool_does_not_leak_during_network_glitches() throws Exception {
        wireMock.register(responseOk());

        warmUp(5);
        saboteur.addFault(delay("network-delay").delay(500, MILLISECONDS).variance(250, MILLISECONDS));
        runWithRate(5, 100, SECONDS, "Run with broken network");

        int leaked = metricsSource.fetch()
                .gauge("org.apache.http.conn.HttpClientConnectionManager.pinger-client.leased-connections");

        assertThat(leaked, is(0));

    }

    private static void runWithRate(int period, int rate, TimeUnit unit, String NAME) {
        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());
        crashLab.run(period(period, unit), rate(rate).per(unit), new HttpSteps(NAME) {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });
    }
    private static void warmUp(int forSeconds) {
        final String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());
        crashLab.run(period(forSeconds, SECONDS), rate(20).per(SECONDS), new HttpSteps("warm-up") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });
    }

    private MappingBuilder responseOk() {
        return get(urlEqualTo("/gamble"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(
                        aResponse()
                            .withBody(EXPECTED)
                            .withHeader("Content-Type", "application/json")
                            .withStatus(200)
                            .withFixedDelay(200)
                            //.withTransformers(DistributedResopnseTimeTransformer.NAME)
                );
    }
    private static final String EXPECTED = "{\"answer\":\"pong\"}";

}
