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

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.EffectiveWhiteLabeling;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingInfo;
import org.thingsboard.server.common.data.wl.WhiteLabelingScope;
import org.thingsboard.server.common.data.wl.WhiteLabelingSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;

public interface WhiteLabelingService {

    /**
     * Returns settings for the given scope/tenant/type or null if no record exists.
     */
    WhiteLabelingSettings getSettings(WhiteLabelingScope scope, TenantId tenantId, WhiteLabelingType type);

    /**
     * Persists scoped settings. Replaces any existing record for the same (scope,tenantId,type) tuple.
     */
    WhiteLabelingSettings saveSettings(WhiteLabelingSettings settings);

    /**
     * Removes the scoped record. Returns true if a record was deleted.
     */
    boolean deleteSettings(WhiteLabelingScope scope, TenantId tenantId, WhiteLabelingType type);

    /**
     * Removes all white-labeling settings for the given tenant. Used on tenant removal.
     */
    void deleteByTenantId(TenantId tenantId);

    /**
     * Resolves the effective branding for the authenticated UI shell:
     *   defaults <- system general <- tenant general (where defined)
     */
    EffectiveWhiteLabeling resolveEffectiveBranding(TenantId tenantId);

    /**
     * Resolves the public login branding for an HTTP host header.
     * The host MUST be normalized by the caller via {@link DomainUtils#normalizeDomain(String)}.
     */
    LoginWhiteLabelingInfo resolveLoginBranding(String normalizedDomain);

}
