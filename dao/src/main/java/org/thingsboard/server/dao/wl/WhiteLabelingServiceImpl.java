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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.EffectiveWhiteLabeling;
import org.thingsboard.server.common.data.wl.GeneralWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingInfo;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingScope;
import org.thingsboard.server.common.data.wl.WhiteLabelingSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingSettingsEntity;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.sql.wl.WhiteLabelingRepository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhiteLabelingServiceImpl implements WhiteLabelingService {

    private final WhiteLabelingRepository repository;
    private final CacheManager cacheManager;

    @Value("${white_labeling.assets.max_logo_data_uri_size:262144}")
    private int maxLogoDataUriSize;

    @Value("${white_labeling.assets.max_favicon_data_uri_size:65536}")
    private int maxFaviconDataUriSize;

    @Value("${white_labeling.advanced_css.max_length:32768}")
    private int maxAdvancedCssLength;

    @Override
    @Transactional(readOnly = true)
    public WhiteLabelingSettings getSettings(WhiteLabelingScope scope, TenantId tenantId, WhiteLabelingType type) {
        UUID tenantUuid = tenantUuid(scope, tenantId);
        return repository.findByScopeAndTenantIdAndType(scope, tenantUuid, type)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public WhiteLabelingSettings saveSettings(WhiteLabelingSettings settings) {
        if (settings == null || settings.getScope() == null || settings.getType() == null) {
            throw new DataValidationException("White-labeling settings must specify scope and type");
        }
        ConstraintValidator.validateFields(settings);
        validatePayload(settings);

        UUID tenantUuid = tenantUuid(settings.getScope(), settings.getTenantId());
        Optional<WhiteLabelingSettingsEntity> existing =
                repository.findByScopeAndTenantIdAndType(settings.getScope(), tenantUuid, settings.getType());

        WhiteLabelingSettingsEntity entity = existing.orElseGet(() -> {
            WhiteLabelingSettingsEntity e = new WhiteLabelingSettingsEntity();
            e.setCreatedTime(System.currentTimeMillis());
            e.setScope(settings.getScope());
            e.setTenantId(tenantUuid);
            e.setType(settings.getType());
            return e;
        });

        if (settings.getType() == WhiteLabelingType.LOGIN) {
            String normalizedDomain = null;
            if (settings.getDomain() != null && !settings.getDomain().isBlank()) {
                normalizedDomain = DomainUtils.normalizeDomain(settings.getDomain());
                if (normalizedDomain == null) {
                    throw new DataValidationException("Invalid login domain: " + settings.getDomain());
                }
                Optional<WhiteLabelingSettingsEntity> conflict =
                        repository.findFirstByTypeAndDomain(WhiteLabelingType.LOGIN, normalizedDomain);
                if (conflict.isPresent() && !conflict.get().getId().equals(entity.getId())) {
                    throw new DataValidationException("Login domain '" + normalizedDomain + "' is already configured by another scope");
                }
            }
            entity.setDomain(normalizedDomain);
            entity.setSettingsJson(JacksonUtil.toString(settings.getLogin()));
        } else {
            entity.setDomain(null);
            entity.setSettingsJson(JacksonUtil.toString(settings.getGeneral()));
        }

        WhiteLabelingSettingsEntity saved = repository.save(entity);
        evictCache();
        return toDto(saved);
    }

    @Override
    @Transactional
    public boolean deleteSettings(WhiteLabelingScope scope, TenantId tenantId, WhiteLabelingType type) {
        UUID tenantUuid = tenantUuid(scope, tenantId);
        Optional<WhiteLabelingSettingsEntity> existing = repository.findByScopeAndTenantIdAndType(scope, tenantUuid, type);
        if (existing.isEmpty()) {
            return false;
        }
        repository.deleteByScopeAndTenantIdAndType(scope, tenantUuid, type);
        evictCache();
        return true;
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        if (tenantId == null) {
            return;
        }
        repository.deleteByTenantId(tenantId.getId());
        evictCache();
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.WHITE_LABELING_CACHE, key = "'effective:' + #tenantId.id")
    @Transactional(readOnly = true)
    public EffectiveWhiteLabeling resolveEffectiveBranding(TenantId tenantId) {
        GeneralWhiteLabelingParams system = loadGeneral(WhiteLabelingScope.SYSTEM, TenantId.SYS_TENANT_ID);
        GeneralWhiteLabelingParams tenant = (tenantId == null || tenantId.isSysTenantId())
                ? null
                : loadGeneral(WhiteLabelingScope.TENANT, tenantId);
        return mergeGeneral(system, tenant);
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.WHITE_LABELING_CACHE, key = "'login:' + (#normalizedDomain == null ? '' : #normalizedDomain)")
    @Transactional(readOnly = true)
    public LoginWhiteLabelingInfo resolveLoginBranding(String normalizedDomain) {
        LoginWhiteLabelingParams systemLogin = loadLogin(WhiteLabelingScope.SYSTEM, TenantId.SYS_TENANT_ID);
        LoginWhiteLabelingParams tenantLogin = null;
        if (normalizedDomain != null && !normalizedDomain.isEmpty()) {
            Optional<WhiteLabelingSettingsEntity> match =
                    repository.findFirstByTypeAndDomain(WhiteLabelingType.LOGIN, normalizedDomain);
            if (match.isPresent()) {
                tenantLogin = JacksonUtil.fromString(match.get().getSettingsJson(), LoginWhiteLabelingParams.class);
            }
        }
        return projectLogin(mergeLogin(systemLogin, tenantLogin));
    }

    private GeneralWhiteLabelingParams loadGeneral(WhiteLabelingScope scope, TenantId tenantId) {
        UUID tenantUuid = tenantUuid(scope, tenantId);
        return repository.findByScopeAndTenantIdAndType(scope, tenantUuid, WhiteLabelingType.GENERAL)
                .map(e -> JacksonUtil.fromString(e.getSettingsJson(), GeneralWhiteLabelingParams.class))
                .orElse(null);
    }

    private LoginWhiteLabelingParams loadLogin(WhiteLabelingScope scope, TenantId tenantId) {
        UUID tenantUuid = tenantUuid(scope, tenantId);
        return repository.findByScopeAndTenantIdAndType(scope, tenantUuid, WhiteLabelingType.LOGIN)
                .map(e -> JacksonUtil.fromString(e.getSettingsJson(), LoginWhiteLabelingParams.class))
                .orElse(null);
    }

    static EffectiveWhiteLabeling mergeGeneral(GeneralWhiteLabelingParams... layers) {
        EffectiveWhiteLabeling result = new EffectiveWhiteLabeling();
        for (GeneralWhiteLabelingParams layer : layers) {
            if (layer == null) {
                continue;
            }
            if (layer.getApplicationTitle() != null) {
                result.setApplicationTitle(layer.getApplicationTitle());
            }
            String fav = preferUrl(layer.getFaviconUrl(), layer.getFaviconDataUri());
            if (fav != null) {
                result.setFaviconUrl(fav);
            }
            String logo = preferUrl(layer.getLogoUrl(), layer.getLogoDataUri());
            if (logo != null) {
                result.setLogoUrl(logo);
            }
            if (layer.getLogoHeight() != null) {
                result.setLogoHeight(layer.getLogoHeight());
            }
            if (layer.getPrimaryPalette() != null) {
                result.setPrimaryPalette(mergePalette(result.getPrimaryPalette(), layer.getPrimaryPalette()));
            }
            if (layer.getAccentPalette() != null) {
                result.setAccentPalette(mergePalette(result.getAccentPalette(), layer.getAccentPalette()));
            }
            if (layer.getAdvancedCss() != null) {
                result.setAdvancedCss(layer.getAdvancedCss());
            }
            if (layer.getShowPlatformNameAndVersion() != null) {
                result.setShowPlatformNameAndVersion(layer.getShowPlatformNameAndVersion());
            }
        }
        return result;
    }

    static LoginWhiteLabelingParams mergeLogin(LoginWhiteLabelingParams... layers) {
        LoginWhiteLabelingParams result = new LoginWhiteLabelingParams();
        for (LoginWhiteLabelingParams layer : layers) {
            if (layer == null) {
                continue;
            }
            if (layer.getApplicationTitle() != null) {
                result.setApplicationTitle(layer.getApplicationTitle());
            }
            if (layer.getFaviconUrl() != null) {
                result.setFaviconUrl(layer.getFaviconUrl());
            }
            if (layer.getFaviconDataUri() != null) {
                result.setFaviconDataUri(layer.getFaviconDataUri());
            }
            if (layer.getLogoUrl() != null) {
                result.setLogoUrl(layer.getLogoUrl());
            }
            if (layer.getLogoDataUri() != null) {
                result.setLogoDataUri(layer.getLogoDataUri());
            }
            if (layer.getPrimaryPalette() != null) {
                result.setPrimaryPalette(mergePalette(result.getPrimaryPalette(), layer.getPrimaryPalette()));
            }
            if (layer.getAccentPalette() != null) {
                result.setAccentPalette(mergePalette(result.getAccentPalette(), layer.getAccentPalette()));
            }
            if (layer.getBackgroundColor() != null) {
                result.setBackgroundColor(layer.getBackgroundColor());
            }
        }
        return result;
    }

    static LoginWhiteLabelingInfo projectLogin(LoginWhiteLabelingParams params) {
        LoginWhiteLabelingInfo info = new LoginWhiteLabelingInfo();
        if (params == null) {
            return info;
        }
        info.setApplicationTitle(params.getApplicationTitle());
        info.setFaviconUrl(preferUrl(params.getFaviconUrl(), params.getFaviconDataUri()));
        info.setLogoUrl(preferUrl(params.getLogoUrl(), params.getLogoDataUri()));
        info.setPrimaryPalette(params.getPrimaryPalette());
        info.setAccentPalette(params.getAccentPalette());
        info.setBackgroundColor(params.getBackgroundColor());
        return info;
    }

    private static PaletteSettings mergePalette(PaletteSettings base, PaletteSettings override) {
        if (override == null) {
            return base;
        }
        PaletteSettings result = base != null ? new PaletteSettings() : new PaletteSettings();
        if (base != null) {
            result.setName(base.getName());
            result.setMain(base.getMain());
            result.setContrast(base.getContrast());
        }
        if (override.getName() != null) {
            result.setName(override.getName());
        }
        if (override.getMain() != null) {
            result.setMain(override.getMain());
        }
        if (override.getContrast() != null) {
            result.setContrast(override.getContrast());
        }
        return result;
    }

    private static String preferUrl(String url, String dataUri) {
        if (url != null && !url.isBlank()) {
            return url;
        }
        if (dataUri != null && !dataUri.isBlank()) {
            return dataUri;
        }
        return null;
    }

    private static UUID tenantUuid(WhiteLabelingScope scope, TenantId tenantId) {
        if (scope == WhiteLabelingScope.SYSTEM) {
            return TenantId.SYS_TENANT_ID.getId();
        }
        if (tenantId == null) {
            throw new DataValidationException("Tenant id is required for TENANT-scoped white-labeling settings");
        }
        if (tenantId.isSysTenantId()) {
            throw new DataValidationException("Tenant scope must reference a non-system tenant");
        }
        return tenantId.getId();
    }

    private void evictCache() {
        var cache = cacheManager.getCache(CacheConstants.WHITE_LABELING_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private WhiteLabelingSettings toDto(WhiteLabelingSettingsEntity entity) {
        WhiteLabelingSettings dto = new WhiteLabelingSettings();
        dto.setId(entity.getId());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setScope(entity.getScope());
        dto.setType(entity.getType());
        dto.setTenantId(TenantId.fromUUID(entity.getTenantId()));
        dto.setDomain(entity.getDomain());
        if (entity.getSettingsJson() != null) {
            if (entity.getType() == WhiteLabelingType.GENERAL) {
                dto.setGeneral(JacksonUtil.fromString(entity.getSettingsJson(), GeneralWhiteLabelingParams.class));
            } else if (entity.getType() == WhiteLabelingType.LOGIN) {
                dto.setLogin(JacksonUtil.fromString(entity.getSettingsJson(), LoginWhiteLabelingParams.class));
            }
        }
        return dto;
    }

    private void validatePayload(WhiteLabelingSettings settings) {
        if (settings.getType() == WhiteLabelingType.GENERAL) {
            if (settings.getGeneral() == null) {
                throw new DataValidationException("General white-labeling parameters must not be null");
            }
            GeneralWhiteLabelingParams g = settings.getGeneral();
            if (g.getLogoHeight() != null) {
                int h = g.getLogoHeight();
                if (h < 16 || h > 96) {
                    throw new DataValidationException("logoHeight must be between 16 and 96 pixels");
                }
            }
            if (g.getAdvancedCss() != null && g.getAdvancedCss().length() > maxAdvancedCssLength) {
                throw new DataValidationException("advancedCss exceeds the configured maximum length of " + maxAdvancedCssLength + " characters");
            }
            validateDataUri(g.getFaviconDataUri(), maxFaviconDataUriSize, "favicon");
            validateDataUri(g.getLogoDataUri(), maxLogoDataUriSize, "logo");
        } else if (settings.getType() == WhiteLabelingType.LOGIN) {
            if (settings.getLogin() == null) {
                throw new DataValidationException("Login white-labeling parameters must not be null");
            }
            LoginWhiteLabelingParams l = settings.getLogin();
            validateDataUri(l.getFaviconDataUri(), maxFaviconDataUriSize, "favicon");
            validateDataUri(l.getLogoDataUri(), maxLogoDataUriSize, "logo");
        }
    }

    private static void validateDataUri(String dataUri, int maxSize, String fieldName) {
        if (dataUri == null || dataUri.isEmpty()) {
            return;
        }
        if (!dataUri.startsWith("data:")) {
            throw new DataValidationException(fieldName + " inline image must be a data URI starting with 'data:'");
        }
        int comma = dataUri.indexOf(',');
        if (comma < 0) {
            throw new DataValidationException(fieldName + " inline image is not a valid data URI");
        }
        String mediaType = dataUri.substring(5, comma).toLowerCase();
        boolean allowed = mediaType.startsWith("image/png")
                || mediaType.startsWith("image/jpeg")
                || mediaType.startsWith("image/jpg")
                || mediaType.startsWith("image/webp")
                || mediaType.startsWith("image/x-icon")
                || mediaType.startsWith("image/vnd.microsoft.icon")
                || mediaType.startsWith("image/svg+xml");
        if (!allowed) {
            throw new DataValidationException(fieldName + " inline image has an unsupported media type: " + mediaType);
        }
        if (mediaType.startsWith("image/svg+xml") && containsUnsafeSvg(dataUri)) {
            throw new DataValidationException(fieldName + " SVG payload contains script or external references and is not allowed");
        }
        if (dataUri.length() > maxSize) {
            throw new DataValidationException(fieldName + " inline image exceeds the configured maximum size of " + maxSize + " bytes");
        }
    }

    private static boolean containsUnsafeSvg(String dataUri) {
        String lower = dataUri.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") || lower.contains(" onload=") || lower.contains(" onclick=");
    }

}
