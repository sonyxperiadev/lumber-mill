package lumbermill;

import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.transformers.GrokFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;

class Grok<E extends Event> {

    /**
     * Extracts values from a string and adds those as key/value pairs
     *
     * <pre> Groovy usage:
     *  {@code
     * parse (
     *     field : 'message',
     *     pattern : 'AWS_ELB_LOG',
     *     tagOnFailure : false
     * )
     * }</pre>
     */
    public Func1<JsonEvent,Observable<JsonEvent>> parse (Map conf) {
        MapWrap mapWrap = MapWrap.of(conf).assertExists("field", "pattern");
        lumbermill.internal.transformers.Grok grok = GrokFactory.create(mapWrap.asString("field"),
                mapWrap.asString("pattern"),
                mapWrap.get("tagOnFailure", true),
                mapWrap.get("tag", GrokFactory.ERROR_TAG));

        return t -> grok.parse(t);
    }

    public Func1<List<E>, Observable<List<E>>> parseBuffer(Map parameters) {
        MapWrap mapWrap = MapWrap.of(parameters).assertExists("field", "pattern");
        lumbermill.internal.transformers.Grok grok = GrokFactory.create(mapWrap.asString("field"),
                mapWrap.asString("pattern"),
                mapWrap.get("tagOnFailure", true),
                mapWrap.get("tag", GrokFactory.ERROR_TAG));
        return events -> grok.parse(events);
    }
}
