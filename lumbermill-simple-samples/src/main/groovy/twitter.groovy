import lumbermill.Core
import lumbermill.twitter.Twitter

// Prints the message and name of place if place exists

Twitter.feed (
        'consumer_key': '{consumer_key}',
        'consumer_key_secret' : '{consumer_key_secret}',
        'access_token' : '{access_token}',
        'access_token_secret' : '{access_token_secret}'
)
.filter({ev -> ev.status().getPlace() != null})
.doOnNext(Core.console.stdout('Tweet {message} from place {place_full_name}'))
.subscribe()