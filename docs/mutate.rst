Mutating functions
==================

Functions used in the pipeline to mutate/enrich the event contents.

Unless specified, functions are part of the core module which is used by depending on the core module
and importing all methods on the lumbermill.Core class.

.. code-block::groovy

    compile 'com.sonymobile:lumbermill-core:$version'

    import static lumbermill.Core.*


Add / Remove / Rename
---------------------

.. code-block::groovy

    o.flatMap ( addField('name', 'string'))
    o.flatMap ( addField('name', 10))
    o.flatMap ( addField('name', true))
    o.flatMap ( addField('name', 10.8))

    o.flatMap( remove('field'))
    o.flatMap( remove('field1', 'field2'))

    o.flatMap ( rename (from: 'source', to: 'target'))

