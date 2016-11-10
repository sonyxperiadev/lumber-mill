import lumbermill.api.Codecs

import static lumbermill.Core.file
import static lumbermill.ElasticSearch.elasticsearch
import static lumbermill.api.Sys.env


file.readFileAsLines (
        file:  env('file').string(),
        codec : Codecs.TEXT_TO_JSON)

.buffer(env('buffer','10').number())

.flatMap (
    elasticsearch.client(
            basic_auth:   env('user','').string() + ':' + env('passwd','').string(),
            url:          env('es_url').string(),
            index_prefix: 'lumbermill-',
            type:         'fs',
            dispatcher: [
                    max_concurrent_requests: env('max_req','5').number()
            ]
        ))
.doOnError({t -> t.printStackTrace()})
.subscribe()