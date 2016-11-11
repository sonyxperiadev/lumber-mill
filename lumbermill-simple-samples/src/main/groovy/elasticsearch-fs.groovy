import lumbermill.api.Codecs

import static lumbermill.Core.file
import static lumbermill.ElasticSearch.elasticsearch
import static lumbermill.api.Sys.env


file.readFileAsLines (
        file:  '{file}',
        codec : Codecs.TEXT_TO_JSON)

.buffer(env('buffer','10').number())

.flatMap (
    elasticsearch.client(
            basic_auth:   '{user   || }:{passwd || }',
            url:          '{es_url || http://localhost:9200}',
            index_prefix: '{index  || lumbermill}-',
            type:         '{fs     || fs}',
            dispatcher: [
                    max_concurrent_requests: '{max_req || 5}'
            ]
        ))
.doOnError({t -> t.printStackTrace()})
.subscribe()