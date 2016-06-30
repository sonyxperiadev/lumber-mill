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

import groovy.lang.Closure;
import lumbermill.api.AnyJsonEvent;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.JsonParseException;
import lumbermill.internal.MapWrap;
import lumbermill.internal.RetryStrategyImpl;
import lumbermill.internal.StringTemplate;
import lumbermill.internal.transformers.ConditionalFunc1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Contains misc useful stuff
 */
@SuppressWarnings("unused")
public class Core {

    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .toFormatter();

    public static Console console = new Console();
    public static Grok grok = new Grok();
    public static GZip gzip = new GZip();
    public static Zlib zlib = new Zlib();
    public static Base64 base64 = new Base64();
    public static File file = new File();
    public static Fingerprint fingerprint = new Fingerprint();


    /**
     * If the current event is a json array, split that array into multiple JsonEvent.
     */
    public static Func1<AnyJsonEvent, Observable<JsonEvent>> splitIfArray() {
        return e -> e.each().map(jsonEvent -> jsonEvent.withMetaData(e));
    }


    /**
     * Converts from a List into single Events
     */
    public static <T extends Event> Func1<List<T>,Observable<T>> flatten() {
        return ts -> Observable.from(ts);
    }

    /**
     * Parallelizes the call to the specified function with the events in the buffered list.
     * @param func - Target function
     */
    public static <T extends Event> Func1<List<T>,List<T>> parallelize(Func1<T,T> func) {
        return ts -> {

            List<Event> collect = ts.parallelStream()
                    .map(e1 -> func.call(e1))
                    .collect(toList());
            return ts;
        };
    }

    /**
     * Wraps the call in a function taking a buffer (List) as argument and it will invoke the
     * target function sequentially.
     * @param func - Target function
     * @return
     */
    public static <T extends Event> Func1<List<T>,List<T>> sequence(Func1<T, T> func) {
        return ts -> {
            List<Event> collect = ts.stream()
                    .map(e1 -> func.call(e1))
                    .collect(toList());
            return ts;
        };
    }


    /**
     * Extract query parameters from a URL (or parts of a URL) and adds these as field names
     * to the current event.
     *
     * <pre>
     * Expected input: "text?key=value&amp;name=olle&amp;year=2016"
     *
     * Groovy usage:
     *  {@code
     * params (
     *     field : "request",
           keep : ["name","year"]
     * )
     * }
     * </pre>
     */
    public static Func1<JsonEvent, JsonEvent> params(Map conf) {

        MapWrap config = MapWrap.of(conf).assertExists("field");
        String field = config.asString("field");
        List<String> toKeep = config.get("names", new ArrayList<>());

        return jsonEvent -> {
            if (!jsonEvent.has(field)) {
                return jsonEvent;
            }

            String field1 = jsonEvent.valueAsString(field);
            field1 = field1.substring(field1.indexOf("?") + 1);

            String[] strings = field1.split("&");
            for (String string : strings) {
                String[] split = string.split("=");
                if (split.length == 2 && (toKeep.contains(split[0]) || toKeep.isEmpty())) {
                    jsonEvent.put(split[0], split[1]);
                }
            }
            return jsonEvent;
        };
    }

    /***
     * Executes a closure based if the specfied tag does not exists
     *
     * <pre>
     * Groovy usage:
     *  {@code
     * computeIfTagAbsent ('tagname') {
     *    grok.parse (...)
     * }
     * }
     * </pre>
     */
    public  static <E extends Event> Func1<E, Observable<E>> computeIfTagExists(String tag, Closure cmd) {
        return e -> {
            if (e.hasTag(tag) ) {
                return doCompute(cmd, e);
            }
            return Observable.just(e);
        };
    }

    public static Func1<JsonEvent, Observable<JsonEvent>> computeIfTagAbsent(String tag, Closure cmd) {
        return e -> {
            if (!e.hasTag(tag) ) {
                return doCompute(cmd, e);
            }
            return Observable.just(e);
        };
    }

    public static Func1<JsonEvent, Observable<JsonEvent>> computeIfNotMatch(String field, String value, Closure cmd) {
        final StringTemplate template = StringTemplate.compile(value);
        return e -> {
            if (e.has(field) || !e.valueAsString(field).matches(value)) {
                return doCompute(cmd, e);
            }
            return Observable.just(e);
        };
    }

    public static  Func1<JsonEvent, Observable<JsonEvent>> computeIfMatch(String field, String value, Closure cmd) {
        final StringTemplate template = StringTemplate.compile(value);
        return e -> {
            if (e.has(field) && e.valueAsString(field).matches(value)) {
                return doCompute(cmd, e);
            }
            return Observable.just(e);
        };
    }

    public static <E extends Event> Func1<E, Observable<E>> computeIfExists(String field, Closure cmd) {
        return e -> {

            if (!e.has(field)) {
                return Observable.just(e);
            }
            return doCompute(cmd, e);
        };
    }

    public static <E extends Event> Func1<E, Observable<E>> computeIfAbsent(String field, Closure cmd) {
        return e -> {
            if (e.has(field)) {
                return Observable.just(e);
            }
            return doCompute(cmd, e);
        };
    }

    private static <E extends Event> Observable<E> doCompute(Closure cmd, E e) {

            Object closureResult = cmd.call(e);

            // Can be null, just return original event
            if (closureResult == null) {
                return Observable.just(e);
            }
            // Could be an event, return it
            if (closureResult instanceof Event) {
                return Observable.just((E) closureResult);
            }

            // Could be an observable, simply return it
            if (closureResult instanceof Observable) {
                return (Observable)closureResult;
            }

            // Or a function, lets call it and return the result
            if (closureResult instanceof Func1) {
                Object result = ((Func1) closureResult).call(e);
                if (result instanceof Event) {
                    return Observable.just((E)result);
                }
                if (result instanceof Observable) {
                    return (Observable)result;
                }
            }
            LOG.warn("Unable to handle ifAbsent Closure, expected Event or Func1 but got {}",
                    closureResult.getClass());
            return Observable.just(e);

    }

    /**
     * Allows adding a field or calling a function if a field exists.
     *
     * <pre>
     *
     * Groovy usage:
     *  {@code
     * ifExists ('field').map(function)
     * ifExists ('field').add(field:value, field2:value2)
     * )
     * }
     * </pre>
     */
    @Deprecated
    public static ConditionalFunc1 ifExists(String field) {
        return new ConditionalFunc1(event -> event.has(field));
    }

    @Deprecated
    public static ConditionalFunc1 ifNotExists(String field) {
        return new ConditionalFunc1(event -> !event.has(field));
    }

    @Deprecated
    public static ConditionalFunc1 ifMatch(String field, String value) {
        final StringTemplate template = StringTemplate.compile(value);

        return new ConditionalFunc1(event -> {
                if (event.has(field)) {
                    return event.valueAsString(field).matches(value);
                }
                return false;
        });
    }

    @Deprecated
    public static ConditionalFunc1 ifNotMatch(String field, String value) {
        final StringTemplate template = StringTemplate.compile(value);

        return new ConditionalFunc1(event -> {
            if (event.has(field)) {
                return !event.valueAsString(field).matches(value);
            }
            return false;
        });
    }

    public static Func1<JsonEvent, JsonEvent> remove(String... field) {
        return jsonEvent -> jsonEvent.remove(field);
    }

    public static Func1<JsonEvent, JsonEvent> rename(Map map) {
        MapWrap config = MapWrap.of(map).assertExists("from", "to");
        return rename(config.asString("from"), config.asString("to"), false);
    }

    public static Func1<JsonEvent, JsonEvent> copy(Map map) {
        MapWrap config = MapWrap.of(map).assertExists("from", "to");
        return rename(config.asString("from"), config.asString("to"), true);
    }

    private static <E extends Event> Func1<JsonEvent,JsonEvent> rename(String fromField, String toField, boolean copy) {
        return event -> {
            String value = event.valueAsString(fromField);
            if(value != null) {
                event.put(toField, value);
                if (! copy) {
                    event.remove(fromField);
                }
            }
            return event;
        };
    }


    public static Func1<JsonEvent,JsonEvent> timestampNow() {
        return jsonEvent -> jsonEvent.put("@timestamp", ZonedDateTime.now()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromMs(String from) {
        return timestampFromMs(from, "@timestamp");
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromSecs(String from) {
        return timestampFromSecs(from, "@timestamp");
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromMs() {
        return timestampFromMs("@timestamp", "@timestamp");
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromSecs() {
        return timestampFromSecs("@timestamp", "@timestamp");
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromSecs(String from, String to) {
        return e -> {
            if (! e.has(from)) {
                return e;
            }
            String format = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(e.asLong(from) * 1000),
                    ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return e.put (to,
                    format);
        };
    }

    public static Func1<JsonEvent,JsonEvent> timestampFromMs(String from, String to) {
        return e -> {
            if (! e.has(from)) {
                return e;
            }
            String format = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(e.asLong(from)),
                    ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return e.put (to,
                    format);
        };
    }

    /**
     * FIXME - make sure this works properly
     *
     */
    public static Func1<JsonEvent, JsonEvent> date(String field, String... formats) {

        final List<DateTimeFormatter> formatters =
                asList(formats).stream()
                        .map(format -> DateTimeFormatter.ofPattern(format).withZone(ZoneId.of("UTC")))
                        .collect(toList());

        return jsonEvent -> {
            String dateString = jsonEvent.valueAsString(field);
            for (DateTimeFormatter formatter: formatters) {
                try {
                    ZonedDateTime date = ZonedDateTime.parse(dateString, formatter);
                    String isoDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date);
                    jsonEvent.putMetaData("_@date", date);
                    jsonEvent.remove(field);
                    return jsonEvent.put("@timestamp", isoDate);
                } catch (RuntimeException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Failed to parse date {}", e.getMessage());
                    }
                    continue;
                }
            }
            throw new IllegalStateException("Failed to parse date with any of the supplied formats");
        };
    }

    /**
     * Creates a RetryStrategy for the specified exception types AND their subclasses(!).
     * It returns a RetryStrategy and on that you can create which type of retry strategy that you
     * want to use.
     *
     * @see RetryStrategyImpl for details
     *
     *
     */
    public static RetryStrategy exceptionOfType(List<Class<? extends Throwable>> retryOn){
        return new RetryStrategyImpl().retryOn(retryOn);
    }

    /**
     * Same as Core#exceptionOfTypes but with a single exception
     *
     */
    public static RetryStrategy exceptionOfType(Class<? extends Throwable> retryOn){
        return new RetryStrategyImpl().retryOn(retryOn);
    }

    /**
     * Same as Core#exceptionOfTypes but will retry on any exception
     *
     */
    public static RetryStrategy exceptionOfAnyType(){
        return new RetryStrategyImpl();
    }

    public static <E extends Event> Func1<E, Boolean> hasField(String field) {
        return event -> event.has(field);
    }


    /**
     * Adds the specified field and String value to the event
     */
    public static Func1<JsonEvent, JsonEvent> addField(String field, String value) {
        return event ->
            event.put(field, value);
    }

    /**
     * Adds the specified field and int value to the event
     */
    public static Func1<JsonEvent,JsonEvent> addField(String field, int value) {
        return event ->
                event.put(field, value);
    }

    /**
     * Adds the specified field and boolean value to the event
     */
    public static Func1<JsonEvent,JsonEvent> addField(String field, boolean value) {
        return event ->
                event.put(field, value);
    }

    /**
     * Adds the specified field and float value to the event
     */
    public static Func1<JsonEvent,JsonEvent> addField(String field, float value) {
        return event ->
                event.put(field, value);
    }

    public static Func1<List<JsonEvent>, Observable<AnyJsonEvent>> toJsonArray() {
        return events -> Observable.just(AnyJsonEvent.fromJsonEvents(events));
    }

    /**
     * Decodes the event into a json array or node which is useful if you do not know
     * which type it is.
     */
    public static <E extends Event> Func1<E, AnyJsonEvent> json() {
        return e -> Codecs.JSON_ANY.from(e);
    }

    /**
     * Decodes the event into a json object but will create a JsonObject from Codecs.TEXT_TO_JSON if
     * parsing json fails.
     */
    public static <E extends Event> Func1<E, JsonEvent> toJsonObject(Map map) {
        MapWrap conf = MapWrap.of(map)
                .assertExists("create_json_on_failure");

        return e -> {
            try {
                return Codecs.JSON_OBJECT.from(e);
            } catch (JsonParseException ex) {
              if(conf.asBoolean("create_json_on_failure")) {
                  return Codecs.TEXT_TO_JSON.from(e);
              }
                throw ex;
            }
        };
    }

    /**
     * Decodes the event into a json object, use this if your events are json.
     */
    public static <E extends Event> Func1<E, JsonEvent> toJsonObject() {
        return e -> Codecs.JSON_OBJECT.from(e);
    }

    /**
     * Extracts a string value from JsonEvent and merges it with the "parent" event or replaces the current event with
     * the child event. Note that when merging, any field that already exists will be overwritten so make sure to
     * rename these fields before applying this function if they should be kept.
     *
     * <pre>
     * Groovy usage:
     *
     *  {@code
     * .map (
     *     extractJsonObject (field: message, merge:true)
     *  )
     * }
     * Input:
     * {@code
     * {
     *   "message" : "{\"hostname\":\"b1e674549118\",\"level\":\"INFO\",\"message\":\"A log message\",\"@timestamp\":\"2016-06-15T13:26:52.485Z\"}",
     *   "logGroup" : "LogGroup1",
     *   "logStream" : "application-latest",
     *   "@timestamp" : "2016-06-15T13:26:52.49Z"
     * }}
     *
     * Output:
     * {@code
     * {
     *   "hostname" : "b1e674549118",
     *   "level" : "INFO",
     *   "message" : "A log message",
     *   "logGroup" : "LogGroup1",
     *   "logStream" : "application-latest",
     *   "@timestamp" : "2016-06-15T13:26:52.485Z"
     * }}
     * </pre>
     */
    public static  Func1<JsonEvent, JsonEvent> extractJsonObject(Map map) {
        final MapWrap config = MapWrap.of(map).assertExists("field");
        final boolean merge = config.exists("merge")
                ? config.asBoolean("merge")
                : true;
        final boolean ignoreNonJson = config.exists("ignoreNonJson")
                ? config.asBoolean("ignoreNonJson")
                : true;

        return jsonEvent -> {
            String shouldBeJson = jsonEvent.valueAsString(config.asString("field"));
            try {
                JsonEvent childEvent = Codecs.JSON_OBJECT.from(shouldBeJson);
                if (merge) {
                    jsonEvent.merge(childEvent);
                    return jsonEvent;
                }
                return childEvent;
            } catch (JsonParseException e) {
                    if (ignoreNonJson) {
                        LOG.debug("Got json parse when extracting {} from {}", config.asString("field"), jsonEvent.raw().utf8());
                        return jsonEvent;
                    }
                throw e;
            }
        };
    }

    public static  Func1<JsonEvent, JsonEvent> extractJsonObject(String field) {
        return extractJsonObject(MapWrap.of("field", field).toMap());
    }

    /**
     * Logstash default decoding, takes that value and simply puts that under the message field. A @timestamp is also
     * added with current time.
     * <pre>
     * Input: "Hello there I am perhaps an unstructured access log"
     * Output:
     * {@code
     * {
     *    "message" : "Hello there I am perhaps an unstructured access log",
     *    "@timestamp" : "2016-03-26T08:41:36.312+01:00"
     * }
     * }
     * </pre>
     */
    public static <E extends Event> Func1<E, JsonEvent> textToJson() {
        return e -> Codecs.TEXT_TO_JSON.from(e);
    }


    /**
     * Extracts json from a field in the JsonEvent, useful for extracting your data from some kind
     * of envelope.
     */
    public static  Func1<JsonEvent, AnyJsonEvent> jsonOfField(String field) {
        return e -> {
            return e.child(field).withMetaData(e);
        };
    }



    public static <T extends Event> Subscriber<T> wrap(Subscriber<T>... s) {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                asList(s).forEach(subs -> subs.onCompleted());
            }

            @Override
            public void onError(Throwable e) {
                asList(s).forEach(subs -> subs.onError(e));
            }

            @Override
            public void onNext(T t) {
                asList(s).forEach(subs -> subs.onNext(t));
            }
        };
    }

    public static <T extends Event> Func1<T,T> wrap(Func1<T, T> target) {
        return t -> {
            try {
                return target.call(t);
            } catch (Throwable th) {
                throw th;
            }
        };
    }

}
