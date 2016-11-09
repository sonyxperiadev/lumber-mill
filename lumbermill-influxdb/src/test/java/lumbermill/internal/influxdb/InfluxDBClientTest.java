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
package lumbermill.internal.influxdb;

import com.google.common.collect.Lists;
import lumbermill.api.Codecs;
import lumbermill.internal.MapWrap;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class InfluxDBClientTest {

  private InfluxDBClient createClient (Answer answer) {

    return InfluxDBClient.prepareForTest (mapWrap -> {
      InfluxDB mock = mock (InfluxDB.class);
      doAnswer (answer).when (mock).write (any (BatchPoints.class));
      return mock;
    });
  }

  @Test
  public void test_save_metrics () {


    createClient (invocation -> {

        BatchPoints batchPoints = (BatchPoints) invocation.getArguments ()[0];
        if (batchPoints.getDatabase ().equals ("thedatabase")) {
          List<Point> points = batchPoints.getPoints ();
          assertThat (points.size ()).isEqualTo (2);
          assertThat (points.get (0).lineProtocol ()).isEqualTo ("measurem,apiKey=thedatabase count=67.0 9223372036854775807");
        } else if (batchPoints.getDatabase ().equals ("thedatabase2")) {
          List<Point> points = batchPoints.getPoints ();
          assertThat (points.size ()).isEqualTo (1);
      }
        return Void.TYPE;
      }

    ).client (new HashMap<String, Object> () {{
      put ("url", "http://localhost:8086");
      put ("measurement", "measurem");
      put ("db", "{apiKey}");
      put ("user", "root");
      put ("password", "root");
      put ("fields", MapWrap.of ("{metric}", "someValue").toMap ());
      put ("flushSize", 100);
      put ("excludeTags", asList ("@timestamp", "message", "metric", "someValue", "time"));
      put ("time", "time");
      put ("precision", TimeUnit.MILLISECONDS);
    }})
      .call (
        Lists.newArrayList (
          Codecs.TEXT_TO_JSON.from ("hello")
            .put ("apiKey", "thedatabase")
            .put ("metric", "count")
            .put ("someValue", 67)
            .put ("time", 1473073541377000000L),
          Codecs.JSON_OBJECT.from ("{}")
            .put ("apiKey", "thedatabase")
            .put ("metric", "bytes")
            .put ("someValue", 67)
            .put ("@timestamp", "2015-08-30T15:28:11.561+02:00"),

          Codecs.JSON_OBJECT.from ("{}")
            .put ("apiKey", "thedatabase2")
            .put ("metric", "bytes")
            .put ("someValue", 67))

      );
  }
}
