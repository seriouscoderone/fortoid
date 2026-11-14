import { expect, test, type Page } from '@playwright/test';

import { setUpWallet } from './fixtures/roles';

interface Asked { asked: number; kept: boolean }

/** The browser's answer is made the test's own: it counts how often it is asked, and says `agrees` every time. */
async function browserThat(page: Page, agrees: boolean): Promise<void> {
    await page.addInitScript((answer) => {
        const state = { asked: 0, kept: false };
        (window as unknown as { storageAsked: typeof state }).storageAsked = state;
        StorageManager.prototype.persist = async function persist(): Promise<boolean> {
            state.asked += 1;
            state.kept = answer;
            return answer;
        };
        StorageManager.prototype.persisted = async function persisted(): Promise<boolean> {
            return state.kept;
        };
    }, agrees);
}

/** Stands in for the phone app: its bridge is there, and offers nothing. */
async function insideThePhoneApp(page: Page): Promise<void> {
    await page.addInitScript(() => {
        const listeners: Array<(event: { data: string }) => void> = [];
        (window as unknown as Record<string, unknown>)['bridge'] = {
            addEventListener(type: string, listener: (event: { data: string }) => void) {
                if (type === 'message') listeners.push(listener);
            },
            postMessage(raw: string) {
                const request = JSON.parse(raw) as { type?: string; id?: string };
                if (request.type !== 'native_request') return;
                const response = {
                    type: 'native_response', id: request.id, ok: false, error: { code: 'unknown_capability', message: 'none' },
                };
                setTimeout(() => listeners.forEach((listener) => listener({ data: JSON.stringify(response) })), 0);
            },
        };
    });
}

async function asked(page: Page): Promise<Asked> {
    return page.evaluate(() => (window as unknown as { storageAsked: Asked }).storageAsked);
}

async function openSettings(page: Page): Promise<void> {
    await page.getByRole('navigation', { name: 'Vault navigation' }).getByRole('link', { name: 'Settings', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Settings', exact: true, level: 1 })).toBeVisible({ timeout: 60_000 });
}

function keptByTheBrowser(page: Page) {
    return page.locator('.detail-item', { has: page.getByText('Kept by the Browser', { exact: true }) }).locator('dd');
}

test.describe('Keeping the wallet\'s storage', () => {
    test('making a wallet asks the browser once, and Settings says the browser agreed', async ({ page }) => {
        await browserThat(page, true);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(keptByTheBrowser(page)).toHaveText('Yes');
    });

    test('Settings says so when the browser did not agree', async ({ page }) => {
        await browserThat(page, false);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(keptByTheBrowser(page))
            .toHaveText('No. The browser may remove this wallet\'s storage when it runs short of space.');
    });

    test('inside the phone app the wallet still asks, and Settings has no line about a browser', async ({ page }) => {
        await browserThat(page, false);
        await insideThePhoneApp(page);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(page.getByText('Storage Backend', { exact: true })).toBeVisible();
        await expect(page.getByText('Kept by the Browser', { exact: true })).toHaveCount(0);
    });
});
