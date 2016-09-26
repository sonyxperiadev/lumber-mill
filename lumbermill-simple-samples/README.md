# Simple Samples

You will find both java and groovy samples here.

Java samples are prefixed with J since they seems to conflict with groovy files.


## Docker

You can run the samples in here with docker just to try them out

### Kinesis Consumer

```
docker run --rm \ 
 -e streamName=<streamName> \ 
 -v ~/.aws:/root/.aws \
 lifelog/lumber-mill kcl.groovy
```

You can also add the following parameters
```
 -e https_proxy=http(s)://proxy_url  // If you are behind a corporate proxy
 -e dry=true                         // Disables checkpointing
 -e appName=<your_app_name>          // Identifier for your app consuming from kinesis
 -e AWS_PROFILE=<profile_name>       // Specify which AWS profile to use if no default
 -e region=<region>                  // Defaults to eu-west-1
```