import lumbermill.api.Codecs;
import lumbermill.internal.MapWrap;


import static lumbermill.Core.*;

public class JGrok {

    public static void main(String args[]) {

        Codecs.TEXT_TO_JSON.from("1 times")
                .toObservable()
                .flatMap (
                        grok.parse ( MapWrap.of (
                                "field","message",
                                "pattern","%{NUMBER:number:int} %{WORD:text}",
                                "tagOnFailure",true).toMap()
                        )
                )
                .doOnNext(console.stdout())
                .doOnNext(console.stdout("It says {number} in field number"))
                .toBlocking()
                .subscribe();
    }
}
