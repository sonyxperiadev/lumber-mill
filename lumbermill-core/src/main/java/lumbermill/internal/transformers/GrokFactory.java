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
package lumbermill.internal.transformers;

import oi.thekraken.grok.api.exception.GrokException;

import java.io.IOException;
import java.io.InputStreamReader;


public class GrokFactory {

    public static final String ERROR_TAG = "_grokparsefailure";

    public static Grok create(String field, String pattern, boolean shouldtag, String tag) {
        return new Grok(internal(pattern), field, pattern, shouldtag, tag);
    }

    public static Grok create(String field, String pattern, boolean shouldtag) {
        return new Grok(internal(pattern), field, pattern, shouldtag, ERROR_TAG);
    }

    public static Grok create(String field, String pattern) {
        return new Grok(internal(pattern), field, pattern, true, ERROR_TAG);
    }

    private static oi.thekraken.grok.api.Grok internal(String pattern) {
        try {
            oi.thekraken.grok.api.Grok grok =  new oi.thekraken.grok.api.Grok();
            grok.copyPatterns(GROK_TEMPLATE.getPatterns());
            grok.compile(pattern, true);
            return grok;
        } catch (GrokException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Template is created once and files are read, each parse instance is then having its patterns
     * copied from template instead of reading files again.
     */
    private static final oi.thekraken.grok.api.Grok GROK_TEMPLATE = new oi.thekraken.grok.api.Grok();

    static {
        try {
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/patterns").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/aws").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/firewalls").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/haproxy").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/ruby").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/nagios").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/java").openStream()));
            GROK_TEMPLATE.addPatternFromReader(new InputStreamReader(Thread.currentThread()
                    .getContextClassLoader().getResource("patterns/linux-syslog").openStream()));
        } catch (GrokException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
