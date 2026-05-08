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
public class GeneralWhiteLabelingParams implements Serializable {

    @Serial
    private static final long serialVersionUID = -8541732144112811912L;

    @NoXss
    @Length(fieldName = "applicationTitle", max = 255)
    private String applicationTitle;

    @NoXss
    @Length(fieldName = "faviconUrl", max = 1024)
    private String faviconUrl;

    @Schema(description = "Inline favicon image as a data URI; size is bounded by configuration.")
    private String faviconDataUri;

    @NoXss
    @Length(fieldName = "logoUrl", max = 1024)
    private String logoUrl;

    @Schema(description = "Inline logo image as a data URI; size is bounded by configuration.")
    private String logoDataUri;

    @Schema(description = "Logo height in pixels (16-96)")
    private Integer logoHeight;

    private PaletteSettings primaryPalette;

    private PaletteSettings accentPalette;

    @Schema(description = "Optional administrator-controlled CSS injected at runtime.")
    private String advancedCss;

    @Schema(description = "Whether the platform name and version footer is displayed.")
    private Boolean showPlatformNameAndVersion;

}
