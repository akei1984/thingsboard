/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.wl;

import java.net.IDN;
import java.util.regex.Pattern;

public final class DomainUtils {

    public static final int MAX_DOMAIN_LENGTH = 253;

    private static final Pattern ALLOWED_HOST = Pattern.compile("^[a-z0-9](?:[a-z0-9\\-\\.]{0,251}[a-z0-9])?$");

    private DomainUtils() {
    }

    /**
     * Normalizes a host value. Returns null if the host is null/blank or invalid.
     * Performed transformations:
     *   - trim
     *   - lower-case
     *   - strip port
     *   - strip trailing dot
     *   - convert IDN to ASCII (Punycode)
     *   - reject if scheme/path/query, whitespace, control chars, or wildcard markers are present
     *   - reject if it does not match the allowed host pattern.
     */
    public static String normalizeDomain(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains("/") || value.contains("\\") || value.contains("?") || value.contains("#") || value.contains("*")) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c)) {
                return null;
            }
        }
        // strip port
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        // strip trailing dot
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return null;
        }
        try {
            value = IDN.toASCII(value, IDN.ALLOW_UNASSIGNED);
        } catch (IllegalArgumentException e) {
            return null;
        }
        value = value.toLowerCase();
        if (value.length() > MAX_DOMAIN_LENGTH) {
            return null;
        }
        if (!ALLOWED_HOST.matcher(value).matches()) {
            return null;
        }
        return value;
    }

}
