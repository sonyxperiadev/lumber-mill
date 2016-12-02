
import lumbermill.api.Codecs
import rx.Observable

import static lumbermill.Core.*

// Performs simple filtering with boolean expression.

/* Requirements:
  : Valid JavaScript
  : Must evaluate to return a boolean value
*/

// Keep when true
Observable.just (
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Olle'),
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Johan'))
        .filter(keepWhen("'{name}' == 'Johan'"))
        .doOnNext(console.stdout('Name on keepWhen(Johan) is {name}'))
        .subscribe()


// Skip when true
Observable.just (
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Olle'),
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Johan'))

        .filter( skipWhen("'{name}' == 'Johan'"))
        .doOnNext(console.stdout('Name on skipWhen(Johan) is {name}'))
        .subscribe()


// To match array contents, use .contains() method
Observable.just (
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Olle').addTag("niceguy"),
        Codecs.TEXT_TO_JSON.from("Hello").put('name','Johan'))

        .filter( keepWhen("{tags}.contains('niceguy')"))
        .doOnNext(console.stdout('Name of nice guy is {name}'))
        .subscribe()