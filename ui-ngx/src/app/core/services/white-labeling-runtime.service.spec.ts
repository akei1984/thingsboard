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

import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { WhiteLabelingRuntimeService } from './white-labeling-runtime.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { of } from 'rxjs';

const mockWhiteLabelingService = {
  getEffectiveBranding: () => of({}),
  getLoginBranding: () => of({})
};

describe('WhiteLabelingRuntimeService – advanced CSS', () => {
  let service: WhiteLabelingRuntimeService;
  let document: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WhiteLabelingRuntimeService,
        { provide: WhiteLabelingService, useValue: mockWhiteLabelingService }
      ]
    });
    service  = TestBed.inject(WhiteLabelingRuntimeService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    document.getElementById('tb-white-labeling-style')?.remove();
    document.getElementById('tb-white-labeling-vars')?.remove();
    document.getElementById('tb-white-labeling-theme')?.remove();
  });

  it('injects a <style> tag when advancedCss is provided', () => {
    service.applyEffective({ advancedCss: 'body { background: red; }' });

    const el = document.getElementById('tb-white-labeling-style');
    expect(el).toBeTruthy();
    expect(el!.textContent).toContain('body { background: red; }');
  });

  it('removes the <style> tag when advancedCss is cleared', () => {
    service.applyEffective({ advancedCss: 'body { background: red; }' });
    expect(document.getElementById('tb-white-labeling-style')).toBeTruthy();

    service.applyEffective({ advancedCss: '' });
    expect(document.getElementById('tb-white-labeling-style')).toBeNull();
  });

  it('replaces the previous <style> tag on each apply', () => {
    service.applyEffective({ advancedCss: '.old { color: blue; }' });
    service.applyEffective({ advancedCss: '.new { color: green; }' });

    const styles = document.querySelectorAll('#tb-white-labeling-style');
    expect(styles.length).toBe(1);
    expect(styles[0].textContent).toContain('.new { color: green; }');
    expect(styles[0].textContent).not.toContain('.old');
  });

  it('removes the <style> tag on reset()', () => {
    service.applyEffective({ advancedCss: 'a { color: pink; }' });
    service.reset();
    expect(document.getElementById('tb-white-labeling-style')).toBeNull();
  });
});

describe('WhiteLabelingRuntimeService – advanced JavaScript', () => {
  let service: WhiteLabelingRuntimeService;
  let document: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WhiteLabelingRuntimeService,
        { provide: WhiteLabelingService, useValue: mockWhiteLabelingService }
      ]
    });
    service  = TestBed.inject(WhiteLabelingRuntimeService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    document.getElementById('tb-white-labeling-script')?.remove();
  });

  it('injects a <script> tag when advancedJs is provided', () => {
    service.applyEffective({ advancedJs: 'window.__wl_test = 42;' });

    const el = document.getElementById('tb-white-labeling-script');
    expect(el).toBeTruthy();
    expect(el!.textContent).toContain('window.__wl_test = 42;');
  });

  it('removes the <script> tag when advancedJs is cleared', () => {
    service.applyEffective({ advancedJs: 'window.__wl_test = 42;' });
    expect(document.getElementById('tb-white-labeling-script')).toBeTruthy();

    service.applyEffective({ advancedJs: '' });
    expect(document.getElementById('tb-white-labeling-script')).toBeNull();
  });

  it('replaces the previous <script> tag on each apply', () => {
    service.applyEffective({ advancedJs: 'window.__wl_a = 1;' });
    service.applyEffective({ advancedJs: 'window.__wl_b = 2;' });

    const scripts = document.querySelectorAll('#tb-white-labeling-script');
    expect(scripts.length).toBe(1);
    expect(scripts[0].textContent).toContain('window.__wl_b = 2;');
    expect(scripts[0].textContent).not.toContain('window.__wl_a');
  });

  it('removes the <script> tag on reset()', () => {
    service.applyEffective({ advancedJs: 'window.__wl_test = 1;' });
    service.reset();
    expect(document.getElementById('tb-white-labeling-script')).toBeNull();
  });
});

describe('WhiteLabelingRuntimeService – JS libraries', () => {
  let service: WhiteLabelingRuntimeService;
  let document: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WhiteLabelingRuntimeService,
        { provide: WhiteLabelingService, useValue: mockWhiteLabelingService }
      ]
    });
    service  = TestBed.inject(WhiteLabelingRuntimeService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    [0, 1, 2].forEach(i => document.getElementById(`tb-white-labeling-lib-${i}`)?.remove());
    document.getElementById('tb-white-labeling-script')?.remove();
  });

  it('injects a <script src> for each URL-based library', () => {
    service.applyEffective({
      jsLibraries: [
        { name: 'libA', url: 'https://cdn.example.com/a.js' },
        { name: 'libB', url: 'https://cdn.example.com/b.js' }
      ]
    });

    const el0 = document.getElementById('tb-white-labeling-lib-0');
    const el1 = document.getElementById('tb-white-labeling-lib-1');
    expect(el0).toBeTruthy();
    expect((el0 as HTMLScriptElement).src).toBe('https://cdn.example.com/a.js');
    expect(el1).toBeTruthy();
    expect((el1 as HTMLScriptElement).src).toBe('https://cdn.example.com/b.js');
  });

  it('prefers dataUri over url when both are set', () => {
    const dataUri = 'data:text/javascript;base64,Y29uc29sZS5sb2coMSk7';
    service.applyEffective({
      jsLibraries: [{ name: 'inline', url: 'https://cdn.example.com/a.js', dataUri }]
    });

    const el = document.getElementById('tb-white-labeling-lib-0') as HTMLScriptElement;
    expect(el).toBeTruthy();
    expect(el.src).toBe(dataUri);
  });

  it('skips entries with neither url nor dataUri', () => {
    service.applyEffective({ jsLibraries: [{ name: 'empty' }] });
    expect(document.getElementById('tb-white-labeling-lib-0')).toBeNull();
  });

  it('removes all library tags on reset()', () => {
    service.applyEffective({
      jsLibraries: [
        { url: 'https://cdn.example.com/a.js' },
        { url: 'https://cdn.example.com/b.js' }
      ]
    });
    service.reset();
    expect(document.getElementById('tb-white-labeling-lib-0')).toBeNull();
    expect(document.getElementById('tb-white-labeling-lib-1')).toBeNull();
  });
});
