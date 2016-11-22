package lumbermill.twitter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import lumbermill.internal.Json;
import twitter4j.Status;

/**
 *
 * For Experimental purposes only
 *
 * Wraps a twitter4j.Status instance
 */
public class TweetEvent extends JsonEvent {

    private final Status status;

    public static TweetEvent of(Status status) {
        return new TweetEvent(status);
    }

    private TweetEvent(Status status) {
        super(toJson(status));
        this.status = status;
    }

    private static ObjectNode toJson(Status status) {

        // Extract some stuff, custom parsing can be done if needed.
        // This is to supply something

        JsonEvent ev =  Codecs.TEXT_TO_JSON
                .from(status.getText())
                .put("id", status.getId())
                .put("created_time_ms", status.getCreatedAt().toInstant().toEpochMilli())
                .put("lang", status.getLang())
                .put("in_reply_to_screen_name", status.getInReplyToScreenName())
                .put("in_reply_to_status_id", status.getInReplyToStatusId())
                .put("in_reply_to_user_id", status.getInReplyToUserId())
                .put("retweeted_count", status.getRetweetCount())
                .put("favourite_count", status.getFavoriteCount());

        if (status.getUser() != null) {
            ev.put("user_id", status.getUser().getId())
                    .put("user_name", status.getUser().getName());
        }

        if (status.getPlace() != null ) {
            ev.put("place_name", status.getPlace().getName())
                    .put("place_full_name", status.getPlace().getFullName())
                    .put("country", status.getPlace().getCountry())
                    .put("country_code2", status.getPlace().getCountryCode());
        }
        if (status.getGeoLocation() != null) {
            ev.put("longitude", status.getGeoLocation().getLongitude())
                    .put("latitude", status.getGeoLocation().getLatitude())
                    .unsafe().set("location",
                    Json.OBJECT_MAPPER.createArrayNode()
                            .add(status.getGeoLocation().getLongitude())
                            .add(status.getGeoLocation().getLatitude()));
        }
        return ev.unsafe();
    }

    public Status status() {
        return status;
    }
}
