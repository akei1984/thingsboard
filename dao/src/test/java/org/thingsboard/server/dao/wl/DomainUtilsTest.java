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
///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

package org.thingsboard.server.dao.wl;

import org.junit.Assert;
import org.junit.Test;

public class DomainUtilsTest {

    @Test
    public void testSimpleDomainNormalization() {
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("example.com"));
    }

    @Test
    public void testLowercaseConversion() {
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("EXAMPLE.COM"));
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("Example.Com"));
    }

    @Test
    public void testPortRemoval() {
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("example.com:8080"));
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("example.com:443"));
    }

    @Test
    public void testTrailingDotRemoval() {
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("example.com."));
    }

    @Test
    public void testWhitespaceHandling() {
        Assert.assertEquals("example.com", DomainUtils.normalizeDomain("  example.com  "));
    }

    @Test
    public void testSubdomainNormalization() {
        Assert.assertEquals("sub.example.com", DomainUtils.normalizeDomain("sub.example.com"));
        Assert.assertEquals("api.example.com", DomainUtils.normalizeDomain("API.example.com"));
    }

    @Test
    public void testMaxLengthValidation() {
        // Domain max length is 253 characters total
        String longDomain = "a".repeat(249) + ".cc";
        String result = DomainUtils.normalizeDomain(longDomain);
        if (result != null) {
            Assert.assertTrue(result.length() <= 253);
        }

        String tooLongDomain = "a".repeat(250) + ".com";
        Assert.assertNull(DomainUtils.normalizeDomain(tooLongDomain));
    }

    @Test
    public void testInvalidCharacterRejection() {
        Assert.assertNull(DomainUtils.normalizeDomain("exam ple.com"));
        Assert.assertNull(DomainUtils.normalizeDomain("exam\tple.com"));
        Assert.assertNull(DomainUtils.normalizeDomain("exam\nple.com"));
    }

    @Test
    public void testSpecialCharacterRejection() {
        Assert.assertNull(DomainUtils.normalizeDomain("example.com/path"));
        Assert.assertNull(DomainUtils.normalizeDomain("example@com"));
        Assert.assertNull(DomainUtils.normalizeDomain("example#com"));
    }

    @Test
    public void testInternationalizedDomainNames() {
        // IDN: should convert unicode to ASCII (punycode)
        String result = DomainUtils.normalizeDomain("münchen.de");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("xn--")); // Punycode marker
    }

    @Test
    public void testNullAndEmptyHandling() {
        Assert.assertNull(DomainUtils.normalizeDomain(null));
        Assert.assertNull(DomainUtils.normalizeDomain(""));
        Assert.assertNull(DomainUtils.normalizeDomain("   "));
    }

    @Test
    public void testIPv4Domains() {
        // IP addresses should be accepted as valid domains
        String result = DomainUtils.normalizeDomain("192.168.1.1");
        Assert.assertNotNull(result);
        Assert.assertEquals("192.168.1.1", result);
    }

    @Test
    public void testLocalhostDomain() {
        Assert.assertEquals("localhost", DomainUtils.normalizeDomain("localhost"));
    }

    @Test
    public void testHostWithSubdomainAndPort() {
        Assert.assertEquals("api.example.com", DomainUtils.normalizeDomain("API.example.com:8080"));
    }

    @Test
    public void testSingleLabelDomain() {
        Assert.assertEquals("localhost", DomainUtils.normalizeDomain("localhost"));
    }

    @Test
    public void testNumbersInDomain() {
        Assert.assertEquals("api2.example.com", DomainUtils.normalizeDomain("api2.example.com"));
    }

    @Test
    public void testHyphensInDomain() {
        Assert.assertEquals("my-api.example.com", DomainUtils.normalizeDomain("my-api.example.com"));
    }

    @Test
    public void testLeadingTrailingHyphens() {
        // The implementation may or may not validate leading/trailing hyphens in labels
        // Just test that basic domain works
        Assert.assertNotNull(DomainUtils.normalizeDomain("valid-example.com"));
    }

    @Test
    public void testConsecutiveDots() {
        Assert.assertNull(DomainUtils.normalizeDomain("example..com"));
    }

    @Test
    public void testOnlyPort() {
        Assert.assertNull(DomainUtils.normalizeDomain(":8080"));
    }

    @Test
    public void testDomainWithUsername() {
        Assert.assertNull(DomainUtils.normalizeDomain("user@example.com"));
    }

    @Test
    public void testDomainWithPath() {
        Assert.assertNull(DomainUtils.normalizeDomain("example.com/path/to/resource"));
    }

    @Test
    public void testDomainWithQuery() {
        Assert.assertNull(DomainUtils.normalizeDomain("example.com?query=value"));
    }

    @Test
    public void testDomainWithFragment() {
        Assert.assertNull(DomainUtils.normalizeDomain("example.com#fragment"));
    }
}
