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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.EffectiveWhiteLabeling;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingInfo;
import org.thingsboard.server.common.data.wl.WhiteLabelingScope;
import org.thingsboard.server.common.data.wl.WhiteLabelingSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.wl.DomainUtils;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class WhiteLabelingController extends BaseController {

    private final WhiteLabelingService whiteLabelingService;

    @Value("${white_labeling.login.trust_forwarded_host:false}")
    private boolean trustForwardedHost;

    @ApiOperation(value = "Get scoped white-labeling settings (getWhiteLabelingSettings)",
            notes = "Returns the editable settings for the current user's scope (system for SYS_ADMIN, tenant for TENANT_ADMIN). " +
                    "Returns null if no settings have been configured yet.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/whiteLabeling/settings")
    public WhiteLabelingSettings getWhiteLabelingSettings(
            @Parameter(description = "Settings type: GENERAL or LOGIN")
            @RequestParam("type") WhiteLabelingType type) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        WhiteLabelingScope scope = scopeFor(user);
        TenantId tenantId = scope == WhiteLabelingScope.SYSTEM ? TenantId.SYS_TENANT_ID : user.getTenantId();
        return whiteLabelingService.getSettings(scope, tenantId, type);
    }

    @ApiOperation(value = "Save scoped white-labeling settings (saveWhiteLabelingSettings)",
            notes = "Persists branding settings for the current user's scope. SYS_ADMIN saves system-level defaults; " +
                    "TENANT_ADMIN saves tenant-level overrides. The scope and tenant id from the request body are ignored — " +
                    "the server fills them from the security context to avoid cross-tenant writes.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/whiteLabeling/settings")
    public WhiteLabelingSettings saveWhiteLabelingSettings(
            @RequestBody WhiteLabelingSettings settings) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        WhiteLabelingScope scope = scopeFor(user);
        settings.setScope(scope);
        settings.setTenantId(scope == WhiteLabelingScope.SYSTEM ? TenantId.SYS_TENANT_ID : user.getTenantId());
        return checkNotNull(whiteLabelingService.saveSettings(settings));
    }

    @ApiOperation(value = "Reset scoped white-labeling settings (deleteWhiteLabelingSettings)",
            notes = "Removes the editable settings for the current user's scope. After deletion, the effective branding " +
                    "is resolved from the parent scope (or the platform default at system scope).")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/whiteLabeling/settings")
    public void deleteWhiteLabelingSettings(
            @Parameter(description = "Settings type: GENERAL or LOGIN")
            @RequestParam("type") WhiteLabelingType type) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        WhiteLabelingScope scope = scopeFor(user);
        TenantId tenantId = scope == WhiteLabelingScope.SYSTEM ? TenantId.SYS_TENANT_ID : user.getTenantId();
        whiteLabelingService.deleteSettings(scope, tenantId, type);
    }

    @ApiOperation(value = "Get effective UI branding (getEffectiveWhiteLabeling)",
            notes = "Returns the merged branding (system + tenant) for the authenticated UI shell. Available to any " +
                    "authenticated user; customer users receive tenant-effective branding without overrides.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/whiteLabeling/effective")
    public EffectiveWhiteLabeling getEffectiveWhiteLabeling() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getAuthority() == Authority.SYS_ADMIN ? TenantId.SYS_TENANT_ID : user.getTenantId();
        return whiteLabelingService.resolveEffectiveBranding(tenantId);
    }

    @ApiOperation(value = "Get public login branding (getLoginWhiteLabelingInfo)",
            notes = "Resolves login-page branding using the request host. The endpoint is unauthenticated and returns " +
                    "presentation-only fields. Unknown hosts fall back to the system default.")
    @GetMapping(value = "/noauth/whiteLabeling/login")
    public LoginWhiteLabelingInfo getLoginWhiteLabelingInfo(HttpServletRequest request) {
        String host = resolveHost(request);
        String normalized = host == null ? null : DomainUtils.normalizeDomain(host);
        return whiteLabelingService.resolveLoginBranding(normalized);
    }

    private WhiteLabelingScope scopeFor(SecurityUser user) throws ThingsboardException {
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return WhiteLabelingScope.SYSTEM;
        }
        if (user.getAuthority() == Authority.TENANT_ADMIN) {
            return WhiteLabelingScope.TENANT;
        }
        throw new ThingsboardException("Customer users cannot manage white-labeling settings",
                org.thingsboard.server.common.data.exception.ThingsboardErrorCode.PERMISSION_DENIED);
    }

    private String resolveHost(HttpServletRequest request) {
        if (trustForwardedHost) {
            String forwarded = request.getHeader("X-Forwarded-Host");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
        }
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return host;
    }

}
