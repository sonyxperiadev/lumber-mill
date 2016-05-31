package lumbermill.elasticsearch;

import lumbermill.api.Codecs;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


public class ElasticSearchBulkResponseEventTest {

    @Test
    public void test_create_response_from_request() {

        ElasticSearchBulkRequestEvent e = new ElasticSearchBulkRequestEvent(
                asList(Codecs.JSON_OBJECT.from("{\"create\":true}"),
                        Codecs.TEXT_TO_JSON.from("Test"),
                        Codecs.JSON_OBJECT.from("{\"create\":true}"),
                        Codecs.TEXT_TO_JSON.from("Test"),
                        Codecs.JSON_OBJECT.from("{\"create\":true}"),
                        Codecs.TEXT_TO_JSON.from("Test")));

        ElasticSearchBulkResponseEvent response = ElasticSearchBulkResponseEvent.ofPostponed(e);

        //FIXME
       // assertThat(response.child("items").)

    }
}
