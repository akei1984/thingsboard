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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { WhiteLabelingRuntimeService } from '@core/services/white-labeling-runtime.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  defaultGeneralParams,
  defaultLoginParams,
  GeneralWhiteLabelingParams,
  JsLibrary,
  LoginWhiteLabelingParams,
  WhiteLabelingScope,
  WhiteLabelingSettings,
  WhiteLabelingType
} from '@shared/models/white-labeling.models';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-white-labeling-settings',
  templateUrl: './white-labeling-settings.component.html',
  styleUrls: ['./white-labeling-settings.component.scss', './settings-card.scss'],
  standalone: false
})
export class WhiteLabelingSettingsComponent extends PageComponent implements HasConfirmForm, OnInit, OnDestroy {

  generalForm: FormGroup;
  loginForm: FormGroup;

  scope: WhiteLabelingScope = 'TENANT';
  scopeLabel: string;

  generalLoaded = false;
  loginLoaded = false;

  private generalExists = false;
  private loginExists = false;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private wlService: WhiteLabelingService,
              private runtime: WhiteLabelingRuntimeService,
              private translate: TranslateService) {
    super(store);
    const authUser = getCurrentAuthUser(store);
    this.scope = authUser?.authority === Authority.SYS_ADMIN ? 'SYSTEM' : 'TENANT';
    this.scopeLabel = this.translate.instant(this.scope === 'SYSTEM'
      ? 'admin.white-labeling.scope-system'
      : 'admin.white-labeling.scope-tenant');
    this.generalForm = this.buildGeneralForm();
    this.loginForm = this.buildLoginForm();
  }

  ngOnInit() {
    this.loadGeneral();
    this.loadLogin();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirmForm(): FormGroup {
    if (this.generalForm.dirty) {
      return this.generalForm;
    }
    return this.loginForm.dirty ? this.loginForm : this.generalForm;
  }

  saveGeneral(): void {
    const value: GeneralWhiteLabelingParams = this.generalForm.getRawValue();
    const settings: WhiteLabelingSettings = {
      scope: this.scope,
      type: 'GENERAL',
      general: this.normalizeGeneral(value)
    };
    this.wlService.saveSettings(settings).pipe(takeUntil(this.destroy$)).subscribe({
      next: saved => {
        this.processGeneral(saved);
        this.runtime.loadAndApply().subscribe();
        this.notify('admin.white-labeling.saved');
      }
    });
  }

  resetGeneral(): void {
    this.wlService.deleteSettings('GENERAL').pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.generalForm.reset(defaultGeneralParams());
        this.generalExists = false;
        this.runtime.loadAndApply().subscribe();
        this.notify('admin.white-labeling.reset-success');
      }
    });
  }

  discardGeneral(): void {
    if (!this.generalExists) {
      this.generalForm.reset(defaultGeneralParams());
    } else {
      this.loadGeneral();
    }
  }

  saveLogin(): void {
    const value = this.loginForm.getRawValue();
    const settings: WhiteLabelingSettings = {
      scope: this.scope,
      type: 'LOGIN',
      domain: value.domain || undefined,
      login: this.normalizeLogin(value as LoginWhiteLabelingParams)
    };
    this.wlService.saveSettings(settings).pipe(takeUntil(this.destroy$)).subscribe({
      next: saved => {
        this.processLogin(saved);
        this.notify('admin.white-labeling.saved');
      }
    });
  }

  resetLogin(): void {
    this.wlService.deleteSettings('LOGIN').pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loginForm.reset(this.defaultLoginFormValue());
        this.loginExists = false;
        this.notify('admin.white-labeling.reset-success');
      }
    });
  }

  discardLogin(): void {
    if (!this.loginExists) {
      this.loginForm.reset(this.defaultLoginFormValue());
    } else {
      this.loadLogin();
    }
  }

  onLogoSelected(event: Event, formControlName: string, target: 'general' | 'login'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    const form = target === 'general' ? this.generalForm : this.loginForm;
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      form.get(formControlName)?.setValue(result);
      form.get(formControlName)?.markAsDirty();
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  clearAsset(formControlName: string, target: 'general' | 'login'): void {
    const form = target === 'general' ? this.generalForm : this.loginForm;
    form.get(formControlName)?.setValue('');
    form.get(formControlName)?.markAsDirty();
  }

  resolveLogoPreview(value: GeneralWhiteLabelingParams | LoginWhiteLabelingParams): string | null {
    return value.logoUrl?.trim() || value.logoDataUri?.trim() || null;
  }

  resolveFaviconPreview(value: GeneralWhiteLabelingParams | LoginWhiteLabelingParams): string | null {
    return value.faviconUrl?.trim() || value.faviconDataUri?.trim() || null;
  }

  get jsLibraries(): FormArray {
    return this.generalForm.get('jsLibraries') as FormArray;
  }

  addJsLibrary(): void {
    this.jsLibraries.push(this.fb.group({ name: [''], url: [''], dataUri: [''] }));
    this.generalForm.markAsDirty();
  }

  removeJsLibrary(index: number): void {
    this.jsLibraries.removeAt(index);
    this.generalForm.markAsDirty();
  }

  onJsLibraryFileSelected(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    const row = this.jsLibraries.at(index) as FormGroup;
    if (!row.get('name')?.value?.trim()) {
      row.get('name')?.setValue(file.name);
    }
    const reader = new FileReader();
    reader.onload = () => {
      row.get('dataUri')?.setValue(reader.result as string);
      row.get('url')?.setValue('');
      this.generalForm.markAsDirty();
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  testJs(): void {
    const code: string = this.generalForm.get('advancedJs')?.value?.trim() || '';
    if (!code) {
      return;
    }
    try {
      // eslint-disable-next-line no-new-func
      new Function(code)();
    } catch (e: any) {
      this.store.dispatch(new ActionNotificationShow({
        message: e?.message ?? String(e),
        type: 'error',
        duration: 5000,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
      return;
    }
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('admin.white-labeling.js-test-success'),
      type: 'success',
      duration: 3000,
      verticalPosition: 'bottom',
      horizontalPosition: 'left'
    }));
  }

  private buildGeneralForm(): FormGroup {
    return this.fb.group({
      applicationTitle: [''],
      faviconUrl: [''],
      faviconDataUri: [''],
      logoUrl: [''],
      logoDataUri: [''],
      primaryPalette: this.fb.group({
        name: ['custom'],
        main: [''],
        contrast: ['']
      }),
      advancedCss: [''],
      advancedJs: [''],
      jsLibraries: this.fb.array([])
    });
  }

  private buildLoginForm(): FormGroup {
    return this.fb.group({
      domain: [''],
      applicationTitle: [''],
      faviconUrl: [''],
      faviconDataUri: [''],
      logoUrl: [''],
      logoDataUri: [''],
      primaryPalette: this.fb.group({
        name: ['custom'],
        main: [''],
        contrast: ['']
      }),
      backgroundColor: ['']
    });
  }

  private defaultLoginFormValue() {
    return { domain: '', ...defaultLoginParams() };
  }

  private loadGeneral(): void {
    this.wlService.getSettings('GENERAL').pipe(takeUntil(this.destroy$)).subscribe(settings => {
      this.processGeneral(settings);
    });
  }

  private loadLogin(): void {
    this.wlService.getSettings('LOGIN').pipe(takeUntil(this.destroy$)).subscribe(settings => {
      this.processLogin(settings);
    });
  }

  private processGeneral(settings: WhiteLabelingSettings | null): void {
    const params = settings?.general ?? null;
    this.generalExists = !!params;
    const value = { ...defaultGeneralParams(), ...(params ?? {}) };
    const libs: JsLibrary[] = value.jsLibraries ?? [];
    const libArray = this.generalForm.get('jsLibraries') as FormArray;
    libArray.clear({ emitEvent: false });
    libs.forEach(lib => libArray.push(
      this.fb.group({ name: [lib.name ?? ''], url: [lib.url ?? ''], dataUri: [lib.dataUri ?? ''] }),
      { emitEvent: false }
    ));
    this.generalForm.reset({ ...value, jsLibraries: [] }, { emitEvent: false });
    this.generalLoaded = true;
  }

  private processLogin(settings: WhiteLabelingSettings | null): void {
    if (settings && settings.login) {
      this.loginExists = true;
      this.loginForm.reset({ domain: settings.domain || '', ...defaultLoginParams(), ...settings.login });
    } else {
      this.loginExists = false;
      this.loginForm.reset(this.defaultLoginFormValue());
    }
    this.loginLoaded = true;
  }

  private normalizeGeneral(params: GeneralWhiteLabelingParams): GeneralWhiteLabelingParams {
    return {
      applicationTitle: emptyToUndefined(params.applicationTitle),
      faviconUrl: emptyToUndefined(params.faviconUrl),
      faviconDataUri: emptyToUndefined(params.faviconDataUri),
      logoUrl: emptyToUndefined(params.logoUrl),
      logoDataUri: emptyToUndefined(params.logoDataUri),
      primaryPalette: stripPalette(params.primaryPalette),
      advancedCss: emptyToUndefined(params.advancedCss),
      advancedJs: emptyToUndefined(params.advancedJs),
      jsLibraries: params.jsLibraries?.filter(lib => lib.url?.trim() || lib.dataUri?.trim()) ?? undefined
    };
  }

  private normalizeLogin(params: LoginWhiteLabelingParams): LoginWhiteLabelingParams {
    return {
      applicationTitle: emptyToUndefined(params.applicationTitle),
      faviconUrl: emptyToUndefined(params.faviconUrl),
      faviconDataUri: emptyToUndefined(params.faviconDataUri),
      logoUrl: emptyToUndefined(params.logoUrl),
      logoDataUri: emptyToUndefined(params.logoDataUri),
      primaryPalette: stripPalette(params.primaryPalette),
      backgroundColor: emptyToUndefined(params.backgroundColor)
    };
  }

  private notify(messageKey: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type: 'success',
      duration: 3000,
      verticalPosition: 'bottom',
      horizontalPosition: 'left'
    }));
  }

}

function emptyToUndefined(value: string | undefined | null): string | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed.length === 0 ? undefined : trimmed;
}

function stripPalette(palette?: { name?: string; main?: string; contrast?: string }) {
  if (!palette) {
    return undefined;
  }
  const main = emptyToUndefined(palette.main);
  const contrast = emptyToUndefined(palette.contrast);
  if (!main && !contrast) {
    return undefined;
  }
  return {
    name: emptyToUndefined(palette.name) ?? 'custom',
    main,
    contrast
  };
}
