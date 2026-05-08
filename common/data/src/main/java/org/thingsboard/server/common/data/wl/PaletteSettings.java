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
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.io.Serializable;

@Schema
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaletteSettings implements Serializable {

    @Serial
    private static final long serialVersionUID = 4598302947513212018L;

    @NoXss
    private String name;

    @NoXss
    @Schema(description = "Main color in hex notation, e.g. '#1976d2'")
    private String main;

    @NoXss
    @Schema(description = "Contrast color in hex notation, e.g. '#ffffff'")
    private String contrast;

}
