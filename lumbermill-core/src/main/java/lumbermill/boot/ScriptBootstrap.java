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
package lumbermill.boot;

import groovy.lang.GroovyShell;
import lumbermill.internal.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Runs the groovy script
 */
public class ScriptBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptBootstrap.class);

    public static void main (String args[]) throws IOException {

        if (args.length != 1) {
            LOG.info("Must have at least one config file as parameter");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            LOG.info("File does not exist {}", file);
            return;
        }

        LOG.info("Starting pipeline from file {}", file);

        try {
            new ScriptBootstrap().loadFromScriptAsFile(file);
         } catch (RuntimeException e) {
            LOG.error("Unable to create pipeline", e);
            throw new IllegalStateException(e);
        }
    }

    public void loadFromScriptAsFile(File file) {
        try {

            loadFromScriptAsString(new String(Files.readAllBytes(Paths.get(file.toURI()))));
        } catch (IOException e) {
            LOG.error("Unable to create pipeline", e);
            throw new IllegalStateException(e);
        } catch (RuntimeException e) {
            LOG.error("Unable to create pipeline", e);
            throw new IllegalStateException(e);
        }
    }

    public void loadScriptAsResource(URL url) {
        try {
            InputStream inputStream = url.openStream();
            loadFromScriptAsString(Streams.read(inputStream).utf8());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }



    private void loadFromScriptAsString(String script)  {
         GroovyShell shell = new GroovyShell();
         shell.evaluate(script);
     }
}
