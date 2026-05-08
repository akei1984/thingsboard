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

import { Injectable, RendererFactory2, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  EffectiveWhiteLabeling,
  LoginWhiteLabelingInfo
} from '@shared/models/white-labeling.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

const STYLE_ELEMENT_ID = 'tb-white-labeling-style';
const VAR_STYLE_ELEMENT_ID = 'tb-white-labeling-vars';
const SCRIPT_ELEMENT_ID = 'tb-white-labeling-script';
const LIB_ELEMENT_PREFIX = 'tb-white-labeling-lib-';

@Injectable({ providedIn: 'root' })
export class WhiteLabelingRuntimeService {

  private renderer: Renderer2;
  private readonly defaultTitle: string;

  private readonly effective$ = new BehaviorSubject<EffectiveWhiteLabeling | null>(null);
  private readonly login$ = new BehaviorSubject<LoginWhiteLabelingInfo | null>(null);

  constructor(rendererFactory: RendererFactory2,
              private whiteLabelingService: WhiteLabelingService,
              @Inject(DOCUMENT) private document: Document) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.defaultTitle = this.document.title;
  }

  /** Stream of the most recently applied authenticated branding. */
  effective(): Observable<EffectiveWhiteLabeling | null> {
    return this.effective$.asObservable();
  }

  /** Stream of the most recently applied login branding. */
  login(): Observable<LoginWhiteLabelingInfo | null> {
    return this.login$.asObservable();
  }

  /** Fetches the effective branding for the authenticated shell and applies it. */
  loadAndApply(): Observable<EffectiveWhiteLabeling> {
    return new Observable<EffectiveWhiteLabeling>((subscriber) => {
      const sub = this.whiteLabelingService.getEffectiveBranding({ ignoreErrors: true })
        .pipe(catchError(() => of({} as EffectiveWhiteLabeling)))
        .subscribe(branding => {
          this.applyEffective(branding);
          subscriber.next(branding);
          subscriber.complete();
        });
      return () => sub.unsubscribe();
    });
  }

  /** Fetches the login branding (no-auth) and applies it. */
  loadAndApplyLogin(): Observable<LoginWhiteLabelingInfo> {
    return new Observable<LoginWhiteLabelingInfo>((subscriber) => {
      const sub = this.whiteLabelingService.getLoginBranding({ ignoreErrors: true })
        .pipe(catchError(() => of({} as LoginWhiteLabelingInfo)))
        .subscribe(branding => {
          this.applyLogin(branding);
          subscriber.next(branding);
          subscriber.complete();
        });
      return () => sub.unsubscribe();
    });
  }

  /** Applies the given branding to the live document. */
  applyEffective(branding: EffectiveWhiteLabeling | null | undefined): void {
    const safe = branding || {};
    this.effective$.next(safe);
    this.applyTitle(safe.applicationTitle);
    this.applyFavicon(safe.faviconUrl);
    this.applyAdvancedCss(safe.advancedCss);
    this.applyJsLibraries(safe.jsLibraries);
    this.applyAdvancedJs(safe.advancedJs);
    this.applyVars({
      '--tb-wl-logo-url': safe.logoUrl ? `url('${cssEscape(safe.logoUrl)}')` : null,
      '--tb-wl-primary-main': safe.primaryPalette?.main || null,
      '--tb-wl-primary-contrast': safe.primaryPalette?.contrast || null
    });
    this.applyThemeColors(safe.primaryPalette);
  }

  /** Applies login-page branding. */
  applyLogin(branding: LoginWhiteLabelingInfo | null | undefined): void {
    const safe = branding || {};
    this.login$.next(safe);
    this.applyTitle(safe.applicationTitle);
    this.applyFavicon(safe.faviconUrl);
    this.applyVars({
      '--tb-wl-login-logo-url': safe.logoUrl ? `url('${cssEscape(safe.logoUrl)}')` : null,
      '--tb-wl-login-bg': safe.backgroundColor || null,
      '--tb-wl-primary-main': safe.primaryPalette?.main || null,
      '--tb-wl-primary-contrast': safe.primaryPalette?.contrast || null
    });
    this.applyThemeColors(safe.primaryPalette);
  }

  /** Resets DOM-level customizations to defaults. */
  reset(): void {
    this.document.title = this.defaultTitle;
    this.removeElementById(STYLE_ELEMENT_ID);
    this.removeElementById(VAR_STYLE_ELEMENT_ID);
    this.removeElementById(SCRIPT_ELEMENT_ID);
    this.removeJsLibraryElements();
    this.effective$.next(null);
    this.login$.next(null);
  }

  private applyTitle(title?: string): void {
    if (title && title.trim().length > 0) {
      this.document.title = title;
    } else {
      this.document.title = this.defaultTitle;
    }
  }

  private applyFavicon(faviconUrl?: string): void {
    const head = this.document.head;
    if (!head) {
      return;
    }
    const existing = head.querySelectorAll('link[rel~="icon"]');
    existing.forEach(el => head.removeChild(el));
    if (faviconUrl) {
      const link = this.renderer.createElement('link');
      this.renderer.setAttribute(link, 'rel', 'icon');
      this.renderer.setAttribute(link, 'href', faviconUrl);
      this.renderer.appendChild(head, link);
    } else {
      const link = this.renderer.createElement('link');
      this.renderer.setAttribute(link, 'rel', 'icon');
      this.renderer.setAttribute(link, 'href', 'assets/thingsboard.ico');
      this.renderer.appendChild(head, link);
    }
  }

  private applyJsLibraries(libraries?: { name?: string; url?: string; dataUri?: string }[]): void {
    this.removeJsLibraryElements();
    if (!libraries?.length) {
      return;
    }
    libraries.forEach((lib, i) => {
      const src = lib.dataUri?.trim() || lib.url?.trim();
      if (!src) {
        return;
      }
      const script = this.renderer.createElement('script');
      this.renderer.setAttribute(script, 'id', `${LIB_ELEMENT_PREFIX}${i}`);
      this.renderer.setAttribute(script, 'type', 'text/javascript');
      this.renderer.setAttribute(script, 'src', src);
      this.renderer.appendChild(this.document.body, script);
    });
  }

  private removeJsLibraryElements(): void {
    let i = 0;
    while (true) {
      const el = this.document.getElementById(`${LIB_ELEMENT_PREFIX}${i}`);
      if (!el) {
        break;
      }
      el.parentNode!.removeChild(el);
      i++;
    }
  }

  private applyAdvancedJs(js?: string): void {
    this.removeElementById(SCRIPT_ELEMENT_ID);
    if (!js?.trim()) {
      return;
    }
    const script = this.renderer.createElement('script');
    this.renderer.setAttribute(script, 'id', SCRIPT_ELEMENT_ID);
    this.renderer.setAttribute(script, 'type', 'text/javascript');
    this.renderer.appendChild(script, this.renderer.createText(js));
    this.renderer.appendChild(this.document.body, script);
  }

  private applyAdvancedCss(css?: string): void {
    this.removeElementById(STYLE_ELEMENT_ID);
    if (!css || css.trim().length === 0) {
      return;
    }
    const style = this.renderer.createElement('style');
    this.renderer.setAttribute(style, 'id', STYLE_ELEMENT_ID);
    this.renderer.setAttribute(style, 'type', 'text/css');
    this.renderer.appendChild(style, this.renderer.createText(css));
    this.renderer.appendChild(this.document.head, style);
  }

  private applyVars(vars: Record<string, string | null>): void {
    this.removeElementById(VAR_STYLE_ELEMENT_ID);
    const declarations = Object.entries(vars)
      .filter(([, v]) => v !== null && v !== undefined && v !== '')
      .map(([k, v]) => `${k}: ${v};`)
      .join(' ');
    if (!declarations) {
      return;
    }
    const style = this.renderer.createElement('style');
    this.renderer.setAttribute(style, 'id', VAR_STYLE_ELEMENT_ID);
    this.renderer.setAttribute(style, 'type', 'text/css');
    this.renderer.appendChild(style, this.renderer.createText(`:root { ${declarations} }`));
    this.renderer.appendChild(this.document.head, style);
  }

  private removeElementById(id: string): void {
    const existing = this.document.getElementById(id);
    if (existing && existing.parentNode) {
      existing.parentNode.removeChild(existing);
    }
  }

  private applyThemeColors(primaryPalette?: { main?: string; contrast?: string }): void {
    this.removeElementById('tb-white-labeling-theme');
    const bg = primaryPalette?.main?.trim() || null;
    const fg = primaryPalette?.contrast?.trim() || null;
    if (!bg && !fg) {
      return;
    }
    const rules: string[] = [];
    if (bg) {
      rules.push(`
        :root {
          --mdc-theme-primary: ${bg};
          --mat-toolbar-container-background-color: ${bg};
        }
        .tb-nav-header-toolbar,
        .tb-side-menu-toolbar,
        .tb-primary-toolbar {
          background-color: ${bg} !important;
        }
        .mat-mdc-raised-button.mat-primary {
          --mdc-theme-primary: ${bg};
        }
        mat-sidenav-content a:not(.mat-mdc-button-base, .mdc-tab) {
          color: ${bg} !important;
          border-bottom-color: rgba(${this.hexToRgb(bg)}, 0.25) !important;
        }
        mat-sidenav-content a:hover:not(.mat-mdc-button-base, .mdc-tab),
        mat-sidenav-content a:focus:not(.mat-mdc-button-base, .mdc-tab) {
          border-bottom-color: ${bg} !important;
        }
      `);
    }
    if (fg) {
      rules.push(`
        :root {
          --mat-toolbar-container-text-color: ${fg};
          --mat-icon-button-icon-color: ${fg};
          --mdc-icon-button-icon-color: ${fg};
        }
        .tb-nav-header-toolbar,
        .tb-side-menu-toolbar,
        .tb-primary-toolbar {
          color: ${fg} !important;
        }
        .tb-nav-header-toolbar .mat-icon,
        .tb-side-menu-toolbar .mat-icon,
        .tb-primary-toolbar .mat-icon,
        .tb-nav-header-toolbar tb-icon,
        .tb-side-menu-toolbar tb-icon,
        .tb-primary-toolbar tb-icon {
          color: ${fg} !important;
        }
        mat-sidenav a,
        mat-sidenav tb-icon {
          color: ${fg} !important;
        }
      `);
    }
    if (bg && fg) {
      rules.push(`
        .mat-mdc-raised-button.mat-primary { --mdc-theme-on-primary: ${fg}; }
      `);
    }
    const style = this.renderer.createElement('style');
    this.renderer.setAttribute(style, 'id', 'tb-white-labeling-theme');
    this.renderer.setAttribute(style, 'type', 'text/css');
    this.renderer.appendChild(style, this.renderer.createText(rules.join('\n')));
    this.renderer.appendChild(this.document.head, style);
  }

  private hexToRgb(hex: string): string {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (result) {
      return `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`;
    }
    return '0, 0, 0';
  }
}

function cssEscape(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}
