/**
 * The MIT License (MIT)

 * Copyright (c) 2015 inreachventures

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/**
 * This file originates from https://github.com/inreachventures/aws-signing-request-interceptor
 * with some minor changes.
 *
 * Originally Inspired By: http://pokusak.blogspot.co.uk/2015/10/aws-elasticsearch-request-signing.html
 *
 */
package lumbermill.internal.aws;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lumbermill.internal.elasticsearch.RequestSigner;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;


public class AWSV4SignerImpl implements RequestSigner {

    private final static char[] BASE16MAP = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SLASH = "/";
    private static final String X_AMZ_DATE = "x-amz-date";
    private static final String RETURN = "\n";
    private static final String AWS4_HMAC_SHA256 = "AWS4-HMAC-SHA256\n";
    private static final String AWS4_REQUEST = "/aws4_request";
    private static final String AWS4_HMAC_SHA256_CREDENTIAL = "AWS4-HMAC-SHA256 Credential=";
    private static final String SIGNED_HEADERS = ", SignedHeaders=";
    private static final String SIGNATURE = ", Signature=";
    private static final String SHA_256 = "SHA-256";
    private static final String AWS4 = "AWS4";
    private static final String AWS_4_REQUEST = "aws4_request";
    private static final Joiner JOINER = Joiner.on(';');
    private static final String CONNECTION = "connection";
    private static final String CLOSE = ":close";
    public static final DateTimeFormatter BASIC_TIME_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('Z')
            .toFormatter();
    private static final String EMPTY = "";
    private static final String ZERO = "0";
    private static final Joiner AMPERSAND_JOINER = Joiner.on('&');
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String AUTHORIZATION = "Authorization";
    private static final String SESSION_TOKEN = "x-amz-security-token";
    private static final String DATE = "date";

    private final AWSCredentialsProvider credentialsProvider;
    private final String region;
    private final String service;

    private final Supplier<LocalDateTime> clock;

    public AWSV4SignerImpl(AWSCredentialsProvider credentialsProvider,
                           String region,
                           String service,
                           Supplier<LocalDateTime> clock) {
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.service = service;
        this.clock = clock;
    }

    public AWSV4SignerImpl(AWSCredentialsProvider credentialsProvider,
                           String region,
                           String service) {
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.service = service;
        clock =  () ->
                ZonedDateTime.ofInstant (ZonedDateTime.now().toInstant(),
                        ZoneId.of("UTC")).toLocalDateTime();
    }

    public Map<String, String> getSignedHeaders(String uri, String method,
                                                Map<String, String> queryParams,
                                                Map<String, String> headers,
                                                Optional<byte[]> payload) {
        final LocalDateTime now = clock.get();
        final AWSCredentials credentials = credentialsProvider.getCredentials();
        final Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        result.putAll(headers);
        if (!result.containsKey(DATE)) {
            result.put(X_AMZ_DATE, now.format(BASIC_TIME_FORMAT));
        }
        if (AWSSessionCredentials.class.isAssignableFrom(credentials.getClass())) {
            result.put(SESSION_TOKEN, ((AWSSessionCredentials) credentials).getSessionToken());
        }

        final StringBuilder headersString = new StringBuilder();
        final ImmutableList.Builder<String> signedHeaders = ImmutableList.builder();

        for (Map.Entry<String, String> entry : result.entrySet()) {
            headersString.append(headerAsString(entry)).append(RETURN);
            signedHeaders.add(entry.getKey().toLowerCase());
        }

        final String signedHeaderKeys = JOINER.join(signedHeaders.build());
        final String canonicalRequest = method + RETURN +
                uri + RETURN +
                queryParamsString(queryParams) + RETURN +
                headersString.toString() + RETURN +
                signedHeaderKeys + RETURN +
                toBase16(hash(payload.orElse(EMPTY.getBytes(Charsets.UTF_8))));
        final String stringToSign = createStringToSign(canonicalRequest, now);
        final String signature = sign(stringToSign, now, credentials);
        final String autorizationHeader = AWS4_HMAC_SHA256_CREDENTIAL + credentials.getAWSAccessKeyId() + SLASH + getCredentialScope(now) +
                SIGNED_HEADERS + signedHeaderKeys +
                SIGNATURE + signature;

        result.put(AUTHORIZATION, autorizationHeader);
        return ImmutableMap.copyOf(result);
    }

    private String queryParamsString(Map<String, String> queryParams) {
        final ImmutableList.Builder<String> result = ImmutableList.builder();

        for (Map.Entry<String, String> param : new TreeMap<>(queryParams).entrySet()) {
            result.add(param.getKey() + '=' + param.getValue());
        }

        return AMPERSAND_JOINER.join(result.build());
    }

    private String headerAsString(Map.Entry<String, String> header) {
        if (header.getKey().equalsIgnoreCase(CONNECTION)) {
            return CONNECTION + CLOSE;
        }
        if (header.getKey().equalsIgnoreCase(CONTENT_LENGTH) &&
                header.getValue().equals(ZERO)) {
            return header.getKey().toLowerCase() + ':';
        }
        return header.getKey().toLowerCase() + ':' + header.getValue();
    }

    private String sign(String stringToSign, LocalDateTime now, AWSCredentials credentials) {
        return Hex.encodeHexString(hmacSHA256(stringToSign, getSignatureKey(now, credentials)));
    }

    private String createStringToSign(String canonicalRequest, LocalDateTime now) {
        return AWS4_HMAC_SHA256 +
                now.format(BASIC_TIME_FORMAT) + RETURN +
                getCredentialScope(now) + RETURN +
                toBase16(hash(canonicalRequest.getBytes(Charsets.UTF_8)));
    }

    private String getCredentialScope(LocalDateTime now) {
        return now.format(DateTimeFormatter.BASIC_ISO_DATE) + SLASH + region + SLASH + service + AWS4_REQUEST;
    }

    private byte[] hash(byte[] payload) {
        try {
            final MessageDigest md = MessageDigest.getInstance(SHA_256);
            md.update(payload);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    private String toBase16(byte[] data) {
        final StringBuilder hexBuffer = new StringBuilder(data.length * 2);
        for (byte aData : data) {
            hexBuffer.append(BASE16MAP[(aData >> (4)) & 0xF]);
            hexBuffer.append(BASE16MAP[(aData) & 0xF]);
        }
        return hexBuffer.toString();
    }

    private byte[] getSignatureKey(LocalDateTime now, AWSCredentials credentials) {
        final byte[] kSecret = (AWS4 + credentials.getAWSSecretKey()).getBytes(Charsets.UTF_8);
        final byte[] kDate = hmacSHA256(now.format(DateTimeFormatter.BASIC_ISO_DATE), kSecret);
        final byte[] kRegion = hmacSHA256(region, kDate);
        final byte[] kService = hmacSHA256(service, kRegion);
        return hmacSHA256(AWS_4_REQUEST, kService);
    }

    private byte[] hmacSHA256(String data, byte[] key) {
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data.getBytes(Charsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void sign(RequestSigner.SignableRequest request) {
        Map<String, String> signedHeaders = getSignedHeaders( request.uri(),
                request.method(), request.queryParams(), request.headers(), request.payload());
        request.addSignedHeaders(signedHeaders);
    }
}

