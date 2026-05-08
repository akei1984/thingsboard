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
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.io.Serializable;

@Schema
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginWhiteLabelingParams implements Serializable {

    @Serial
    private static final long serialVersionUID = -1827352115518830149L;

    @NoXss
    @Length(fieldName = "applicationTitle", max = 255)
    private String applicationTitle;

    @NoXss
    @Length(fieldName = "faviconUrl", max = 1024)
    private String faviconUrl;

    private String faviconDataUri;

    @NoXss
    @Length(fieldName = "logoUrl", max = 1024)
    private String logoUrl;

    private String logoDataUri;

    private PaletteSettings primaryPalette;

    private PaletteSettings accentPalette;

    @NoXss
    @Length(fieldName = "backgroundColor", max = 32)
    @Schema(description = "Background color in hex notation, e.g. '#ffffff'")
    private String backgroundColor;

}
