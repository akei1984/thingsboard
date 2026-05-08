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

export type WhiteLabelingScope = 'SYSTEM' | 'TENANT';

export type WhiteLabelingType = 'GENERAL' | 'LOGIN';

export interface PaletteSettings {
  name?: string;
  main?: string;
  contrast?: string;
}

export interface JsLibrary {
  name?: string;
  url?: string;
  dataUri?: string;
}

export interface GeneralWhiteLabelingParams {
  applicationTitle?: string;
  faviconUrl?: string;
  faviconDataUri?: string;
  logoUrl?: string;
  logoDataUri?: string;
  primaryPalette?: PaletteSettings;
  advancedCss?: string;
  advancedJs?: string;
  jsLibraries?: JsLibrary[];
}

export interface LoginWhiteLabelingParams {
  applicationTitle?: string;
  faviconUrl?: string;
  faviconDataUri?: string;
  logoUrl?: string;
  logoDataUri?: string;
  primaryPalette?: PaletteSettings;
  backgroundColor?: string;
}

export interface WhiteLabelingSettings {
  id?: string;
  createdTime?: number;
  scope: WhiteLabelingScope;
  type: WhiteLabelingType;
  domain?: string;
  general?: GeneralWhiteLabelingParams;
  login?: LoginWhiteLabelingParams;
}

export interface EffectiveWhiteLabeling {
  applicationTitle?: string;
  faviconUrl?: string;
  logoUrl?: string;
  primaryPalette?: PaletteSettings;
  advancedCss?: string;
  advancedJs?: string;
  jsLibraries?: JsLibrary[];
}

export interface LoginWhiteLabelingInfo {
  applicationTitle?: string;
  faviconUrl?: string;
  logoUrl?: string;
  primaryPalette?: PaletteSettings;
  backgroundColor?: string;
}

export const defaultGeneralParams = (): GeneralWhiteLabelingParams => ({
  applicationTitle: '',
  faviconUrl: '',
  faviconDataUri: '',
  logoUrl: '',
  logoDataUri: '',
  primaryPalette: { name: 'custom', main: '', contrast: '' },
  advancedCss: '',
  advancedJs: '',
  jsLibraries: []
});

export const defaultLoginParams = (): LoginWhiteLabelingParams => ({
  applicationTitle: '',
  faviconUrl: '',
  faviconDataUri: '',
  logoUrl: '',
  logoDataUri: '',
  primaryPalette: { name: 'custom', main: '', contrast: '' },
  backgroundColor: ''
});
