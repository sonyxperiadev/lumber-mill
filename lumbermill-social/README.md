## Twitter Feed

This is intended for experimental purposes only

### Sample

This will write contents to Elasticsearch

```groovy

Twitter.feed (
        'consumer_key': '{consumer_key}',
        'consumer_key_secret' : '{consumer_key_secret}',
        'access_token' : '{access_token}',
        'access_token_secret' : '{access_token_secret}'
        )
        
        .onErrorResumeNext(Observable.empty())

        .filter ({ ev -> 
             ev.status().getPlace() != null
             && ev.status().getGeoLocation() != null
         })

        .map({ ev ->
             ev.putMetaData("fingerprint", UUID.randomUUID().toString())
        })
        .buffer (30, TimeUnit.SECONDS)
        .flatMap (
             elasticsearch.client (
                      basic_auth:   '{basic.auth}',
                      url:          '{elasticsearch}',
                      index_prefix: 'twitter-',
                      type:         'places',
                      document_id:  '{fingerprint}',
                     
                  )
         )
        .subscribe()

```