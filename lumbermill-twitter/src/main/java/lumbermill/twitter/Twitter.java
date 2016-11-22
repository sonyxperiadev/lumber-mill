package lumbermill.twitter;


import lumbermill.internal.MapWrap;
import rx.Observable;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.util.Map;

/**
 * Simple twitter feed
 */
public class Twitter {

    public static Observable<TweetEvent> feed(Map map) {

        final TwitterStream twitterStream = configure(MapWrap.of(map));

        return Observable.create(subscriber -> {

            twitterStream.addListener(new StatusAdapter() {
                @Override
                public void onStatus(Status status) {
                    subscriber.onNext(TweetEvent.of(status));
                }

                @Override
                public void onException(Exception ex) {
                    subscriber.onError(ex);
                }
            });
            twitterStream.sample();
        });
    }

    private static TwitterStream configure(MapWrap mapWrap) {

        mapWrap.assertExists("consumer_key", "consumer_key_secret", "access_token", "access_token_secret");

        TwitterStream stream = TwitterStreamFactory.getSingleton();

        stream.setOAuthConsumer(
                mapWrap.asString("consumer_key"),
                mapWrap.asString("consumer_key_secret"));

        stream.setOAuthAccessToken(new AccessToken(
                mapWrap.asString("access_token"),
                mapWrap.asString("access_token_secret")));

        return stream;
    }

}
