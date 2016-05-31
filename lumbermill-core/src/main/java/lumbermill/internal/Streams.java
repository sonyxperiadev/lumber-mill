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
package lumbermill.internal;

import okio.ByteString;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipInputStream;

import static okio.Okio.buffer;
import static okio.Okio.sink;
import static okio.Okio.source;

/**
 * Utilities for managing files and streams
 */
public class Streams {

    public static void copy(InputStream is, OutputStream out) {
        try {
            buffer(source(is)).readAll(sink(out));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ByteString read(File file) {
        try {
            return ByteString.of(Files.readAllBytes(Paths.get(file.toURI())));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ByteString read(InputStream is) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(is, out);
        return ByteString.of(out.toByteArray());
    }

    /**
     * Forwards to Files.lines() but catches the exception. However,
     * the client MUST close the stream or use try-with-resources.
     */
    public static Stream<String> lines(String file)  {

        List<String> lines = new ArrayList<>();

        try {
            try (BufferedReader r = new BufferedReader(
                    new FileReader(new File(file)))) {
                String line = null;
                while ((line = r.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return lines.stream();
    }

    public static ByteString gzip(ByteString bytes) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes.toByteArray());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
            Streams.copy(in, gzipOutputStream);
            gzipOutputStream.close();
            return ByteString.of(out.toByteArray());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ByteString gunzip(ByteString bytes) {

        try {

            ByteArrayInputStream in = new ByteArrayInputStream(bytes.toByteArray());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            GZIPInputStream gzis =
                    new GZIPInputStream(in);
            Streams.copy(gzis, out);
            return ByteString.of(out.toByteArray());

        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static File gzip(File src) {
        try {
            File target = File.createTempFile("lumbermill", ".gzip");
            FileOutputStream fos = new FileOutputStream(target);
            FileInputStream fis = new FileInputStream(src);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos);
            Streams.copy(fis, gzipOutputStream);
            gzipOutputStream.close();
            return target;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    public static File gunzip(File file) {

        try {
            File uncompressedFile = File.createTempFile("lumbermill", ".gunzip");
            GZIPInputStream gzis =
                    new GZIPInputStream(new FileInputStream(file));
            FileOutputStream decompressed = new FileOutputStream(uncompressedFile);
            Streams.copy(gzis, decompressed);
            return uncompressedFile;

        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }



    public static ByteString read(String file) {
        return read(new File(file));
    }

    public static byte[] zlibCompress(ByteString byteString)  {
        Deflater deflater = new Deflater();
        byte[] data = byteString.toByteArray();
        deflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] output = outputStream.toByteArray();

        return output;
    }

    public static byte[] zlibDecompress(ByteString bytes) {
        try {
            byte[] data = bytes.toByteArray();
            Inflater inflater = new Inflater();
            inflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();

            inflater.end();
            return output;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void write(ByteString bytes, File file) {
        try {
            com.google.common.io.Files.write(bytes.toByteArray(), file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
