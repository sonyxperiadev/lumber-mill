/*
 * Copyright 2016 Sony Mobile Communications, Inc., Inc.
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

/**
 * AWS resources are accessed using this this class.
 */
@SuppressWarnings("unused")
public class AWS {

    public static Kinesis          kinesis       = new Kinesis();
    public static S3               s3            = new S3();
    public static AWSElasticSearch elasticsearch = new AWSElasticSearch();


}
