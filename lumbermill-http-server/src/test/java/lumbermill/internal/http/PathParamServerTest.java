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
package lumbermill.internal.http;

import org.junit.Before;
import lumbermill.Http;

/**
 * Created by 23061174 on 3/7/16.
 */
public class PathParamServerTest extends AbstractHttpServerTest  {

    String contentType = "application/json";
    String path = "/post/:value";

    Http.Server server = null;

    @Before
    public void prepare() {
        server = prepare(path);
    }


}
