Twitter
=======

Twitter feed that is designed for experimental purposes only.

The following fields are found in the TweetEvent json

* message (tweet)
* id
* created_time_ms
* lang
* in_reply_to_screen_name
* in_reply_to_status_id
* in_reply_to_user_id
* retweeted_count
* favourite_count
* user_id
* user_name

Only if user specificed a location
* place_name
* place_full_name
* country
* country_code2
* longitude, double
* latitude, double
* location, [lon, lat] array

There is also full access to the underlying twitter4j api.

.. code-block:: groovy

    import static lumbermill.Core.*
    import lumbermill.social.Twitter

    Twitter.feed (
            'consumer_key': '{consumer_key}',
            'consumer_key_secret' : '{consumer_key_secret}',
            'access_token' : '{access_token}',
            'access_token_secret' : '{access_token_secret}'
    )
    .filter({ev -> ev.status().getPlace() != null})
    .doOnNext(console.stdout('Tweet {message} from place {place_full_name}'))
    .subscribe()