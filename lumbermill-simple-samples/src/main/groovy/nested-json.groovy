import lumbermill.Core
import lumbermill.api.Codecs


// Sample using Template with JsonPointer to read a value
def json = '{"root": {"next": {"leaf":"Hello"}}}'

Codecs.JSON_OBJECT.from(json)
    .toObservable()
    .doOnNext(Core.console.stdout('{/root/next/leaf} World'))
    .doOnNext(Core.console.stdout('{/root/next/leaf || Hi} World'))
    .doOnNext(Core.console.stdout('{/root/next/whatasdf || Hi} World'))
    .subscribe()
