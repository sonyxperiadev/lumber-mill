/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package lumbermill;

import lumbermill.api.Codec;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.internal.MapWrap;
import lumbermill.internal.StringTemplate;
import lumbermill.internal.aws.S3ClientFactory;
import lumbermill.internal.aws.S3ClientImpl;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.Map;
import java.util.Optional;


 public class S3<E extends Event> {

    private static final Logger LOG = LoggerFactory.getLogger(S3.class);

    private final S3ClientFactory clientFactory = new S3ClientFactory();
     
    private Codec<E> defaultEntityCodec = (Codec<E>) Codecs.BYTES;


    /**
     * Downloads an S3 file locally and appends the path under name 'path'
     * <p>
     * 'bucket' and 'key' are mandatory, 'roleArn' is optional
     *
     * <pre>
     * Groovy usage:
     *  {@code
     * observable.flatMap (
     *     s3.download (
     *         bucket:  'the_bucket',
     *         key:     'the_key',
     *         roleArn: 'the_role_arn'
     *     )
     * )
     * }
     * </pre>
     */
    public Func1<E, Observable<E>> download(Map<String, Object> config) {

        MapWrap conf = MapWrap.of(config).assertExists("bucket","key");
        StringTemplate bucket   = conf.asStringTemplate("bucket");
        StringTemplate key      = conf.asStringTemplate("key");
        boolean removeFromS3    = conf.asBoolean("remove", false);
        String outputField      = conf.asString("output_field", "s3_download_path");

        S3ClientImpl client = clientFactory.create(conf);

        return event -> client.download(event, bucket, key, outputField, removeFromS3);

    }

    /**
     * Gets the complete S3Entity as a BytesEvent
     */
    public Func1<E, Observable<E>> get(Map<String, Object> config) {

        MapWrap conf = MapWrap.of(config)
                .assertExists("bucket","key");
        StringTemplate bucket = conf.asStringTemplate("bucket");
        StringTemplate key = conf.asStringTemplate("key");
        Codec<E> codec = conf.getObject("codec", defaultEntityCodec);

        S3ClientImpl client = clientFactory.create(conf);

        return t -> {

            String sBucket = client.format(t, bucket);
            String sKey = client.format(t, key);
            ByteString raw = client.getAsBytes(sBucket, sKey);
            return codec.from(raw)
                    .put("key", sKey)
                    .put("bucket_name", sBucket).toObservable();
        };
    }


     /**
      * Uploads an object to S3, it can be the contents of the event or a reference
      * to a file to upload.
      * <p>
      * 'bucket' and 'key' are mandatory, 'file' and 'roleArn' is optional
      *
      * <pre>
      * Groovy usage:
      *  {@code
      * observable.flatMap (
      *     s3.put (
      *         bucket:  'the_bucket',
      *         key:     'the_key',
      *         roleArn: 'the_role_arn' (optional),
      *         file:    'the_file' (optional, if missing the contents of the event is uploaded)
      *     )
      * )
      * }
      * </pre>
      */
    public <T extends Event> Func1<T, Observable<T>> put(Map<String, Object> config) {

        MapWrap conf = MapWrap.of(config);
        StringTemplate bucketTemplate = conf.asStringTemplate("bucket");
        StringTemplate keyTemplate = conf.asStringTemplate("key");

        Optional<StringTemplate> fileTemplate = conf.exists("file") ?
                Optional.of(StringTemplate.compile(conf.asString("file"))) :
                Optional.empty();

        S3ClientImpl client = clientFactory.create(conf);

        // If file reference exists
        if (fileTemplate.isPresent()) {
            return event -> {
                final String bucket   = client.format(event, bucketTemplate);
                final String key      = client.format(event, keyTemplate);
                final String fileName = client.format(event, fileTemplate.get());
                client.put(fileName, bucket, key);
                return Observable.just(event);
            };
        }

        return t -> {
            client.put(t, bucketTemplate, keyTemplate);
            return Observable.just(t);
        };
    }


}
