package lumbermill.aws.kcl.internal;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import lumbermill.aws.kcl.KCL.Metrics;
import lumbermill.aws.kcl.KCL.UnitOfWorkListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class KinesisConsumerBootstrap {

    private final static Logger LOG = LoggerFactory.getLogger(KinesisConsumerBootstrap.class);

    private final KinesisClientLibConfiguration kinesisCfg;
    private final UnitOfWorkListener unitOfWorkListener;

    private final ExceptionStrategy exceptionStrategy;
    private final Metrics metricsCallback;
    private final boolean dry;

    public KinesisConsumerBootstrap(KinesisClientLibConfiguration kinesisCfg,
                                    UnitOfWorkListener unitOfWorkListener,
                                    ExceptionStrategy exceptionStrategy,
                                    Metrics metricsCallback,
                                    boolean dry) {
        this.kinesisCfg = kinesisCfg;
        this.unitOfWorkListener = unitOfWorkListener;
        this.exceptionStrategy = exceptionStrategy;
        this.metricsCallback = metricsCallback;
        this.dry = dry;
        String httpsProxy = System.getenv("https_proxy");
        if (StringUtils.isNotEmpty(httpsProxy)) {
            URI proxy = URI.create(httpsProxy);
            kinesisCfg.getKinesisClientConfiguration().setProxyHost(proxy.getHost());
            kinesisCfg.getKinesisClientConfiguration().setProxyPort(proxy.getPort());
            kinesisCfg.getDynamoDBClientConfiguration().setProxyHost(proxy.getHost());
            kinesisCfg.getDynamoDBClientConfiguration().setProxyPort(proxy.getPort());
            kinesisCfg.getCloudWatchClientConfiguration().setProxyHost(proxy.getHost());
            kinesisCfg.getCloudWatchClientConfiguration().setProxyPort(proxy.getPort());
        }
    }


    public void start()  {
        int mb = 1024 * 1024;

        LOG.info("Max memory:           {} mb", Runtime.getRuntime().maxMemory() / mb);
        LOG.info("Starting up Kinesis Consumer... (may take a few seconds)");
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(kinesisCfg.getKinesisCredentialsProvider(),
                kinesisCfg.getKinesisClientConfiguration());
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(kinesisCfg.getDynamoDBCredentialsProvider(),
                kinesisCfg.getDynamoDBClientConfiguration());
        AmazonCloudWatch cloudWatchClient = new AmazonCloudWatchClient(kinesisCfg.getCloudWatchCredentialsProvider(),
                kinesisCfg.getCloudWatchClientConfiguration());

        Worker worker = new Worker.Builder()
                .recordProcessorFactory(() -> new RecordProcessor(unitOfWorkListener, exceptionStrategy, metricsCallback, dry))
                .config(kinesisCfg)
                .kinesisClient(kinesisClient)
                .dynamoDBClient(dynamoDBClient)
                .cloudWatchClient(cloudWatchClient)
                .build();

        worker.run();

    }

}
