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
import org.slf4j.LoggerFactory

def logger = LoggerFactory.getLogger('lumbermill.docker')

/*
 * Not a pipeline resource, simply the default script that runs on docker run
 */

logger.info("Welcome to lumber-mill with docker!")
logger.info("Available samples:")
logger.info("docker run --rm lifelog/lumber-mill grok.groovy")
logger.info("docker run --rm lifelog/lumber-mill geoip.groovy")
logger.info("##")
logger.info("To run your own file by specifying the volume:")
logger.info("docker run --rm -v ABSOLUTE_PATH:/samples lifelog/lumber-mill /samples/yourfile.groovy")
logger.info("I.e, run grok from volume (if run from lumber-mill root directory):")
logger.info("docker run --rm -v \$(pwd)/lumbermill-simple-samples/src/main/groovy:/samples lifelog/lumber-mill /samples/grok.groovy")


