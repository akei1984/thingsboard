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

import java.io.Serial;
import java.io.Serializable;

@Schema(description = "Public login-page branding. Returned by the unauthenticated endpoint and contains presentation fields only.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginWhiteLabelingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 2289341204125381112L;

    private String applicationTitle;
    private String faviconUrl;
    private String logoUrl;
    private PaletteSettings primaryPalette;
    private PaletteSettings accentPalette;
    private String backgroundColor;

}
