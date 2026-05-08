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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.wl.WhiteLabelingScope;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.sql.IdGenerator.GeneratedId;

import java.util.UUID;

@Data
@Entity
@Table(name = "white_labeling_settings")
public class WhiteLabelingSettingsEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedId
    private UUID id;

    @Column(name = "created_time", updatable = false)
    private long createdTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private WhiteLabelingScope scope;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WhiteLabelingType type;

    @Column(name = "domain")
    private String domain;

    @Column(name = "settings_json", columnDefinition = "varchar")
    private String settingsJson;

}
