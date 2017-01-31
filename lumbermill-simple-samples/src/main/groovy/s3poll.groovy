import org.slf4j.LoggerFactory

import static lumbermill.AWS.s3
import static lumbermill.Core.*

def logger = LoggerFactory.getLogger("lumbermill-s3-poll");
logger.info("Starting...")

s3.poll(
    bucket: "{S3_POLL_BUCKET}",
    prefix: "files/",
    maxConcurrent: 2,
    notOlderThanInMins: 60
).onFile { f ->
    f.flatMap (
            s3.download(
                    bucket: '{bucketName}',
                    key: '{key}',
                    remove: true
            ))
            .doOnNext(console.stdout())
            .flatMap(
                s3.put(
                        bucket: '{bucketName}',
                        key: 'processed/{key}',
                        file: '{s3_download_path}'
                ))
            //.observeOn(Schedulers.io())
            .doOnNext({a -> Thread.sleep(8000)}) // Simulates long processing...
            .doOnNext({a -> println "done sleeping"})
}
