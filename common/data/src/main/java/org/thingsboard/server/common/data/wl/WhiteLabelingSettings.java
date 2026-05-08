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
package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Schema(description = "White-labeling settings record (one row per scope/type/domain).")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhiteLabelingSettings implements Serializable {

    @Serial
    private static final long serialVersionUID = 7012413012894117612L;

    @Schema(description = "Settings record id (auto-generated UUID).", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private long createdTime;

    @Schema(description = "Configuration scope.", requiredMode = Schema.RequiredMode.REQUIRED)
    private WhiteLabelingScope scope;

    @Schema(description = "Tenant id when scope=TENANT, otherwise system tenant.")
    private TenantId tenantId;

    @Schema(description = "Settings type.", requiredMode = Schema.RequiredMode.REQUIRED)
    private WhiteLabelingType type;

    @NoXss
    @Length(fieldName = "domain", max = 253)
    @Schema(description = "Login domain (LOGIN type only). Lower-case, no port, no scheme.")
    private String domain;

    @Schema(description = "General branding parameters; populated when type=GENERAL.")
    private GeneralWhiteLabelingParams general;

    @Schema(description = "Login branding parameters; populated when type=LOGIN.")
    private LoginWhiteLabelingParams login;

}
