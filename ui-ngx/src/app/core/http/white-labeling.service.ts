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

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import {
  EffectiveWhiteLabeling,
  LoginWhiteLabelingInfo,
  WhiteLabelingSettings,
  WhiteLabelingType
} from '@shared/models/white-labeling.models';

@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  constructor(private http: HttpClient) {}

  getSettings(type: WhiteLabelingType, config?: RequestConfig): Observable<WhiteLabelingSettings | null> {
    const params = new HttpParams().set('type', type);
    return this.http.get<WhiteLabelingSettings | null>('/api/whiteLabeling/settings',
      { ...defaultHttpOptionsFromConfig(config), params });
  }

  saveSettings(settings: WhiteLabelingSettings, config?: RequestConfig): Observable<WhiteLabelingSettings> {
    return this.http.post<WhiteLabelingSettings>('/api/whiteLabeling/settings', settings,
      defaultHttpOptionsFromConfig(config));
  }

  deleteSettings(type: WhiteLabelingType, config?: RequestConfig): Observable<void> {
    const params = new HttpParams().set('type', type);
    return this.http.delete<void>('/api/whiteLabeling/settings',
      { ...defaultHttpOptionsFromConfig(config), params });
  }

  getEffectiveBranding(config?: RequestConfig): Observable<EffectiveWhiteLabeling> {
    return this.http.get<EffectiveWhiteLabeling>('/api/whiteLabeling/effective',
      defaultHttpOptionsFromConfig(config));
  }

  getLoginBranding(config?: RequestConfig): Observable<LoginWhiteLabelingInfo> {
    return this.http.get<LoginWhiteLabelingInfo>('/api/noauth/whiteLabeling/login',
      defaultHttpOptionsFromConfig(config));
  }
}
