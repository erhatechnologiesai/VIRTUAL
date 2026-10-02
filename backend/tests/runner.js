const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

const { AuthService } = require('../dist/auth/auth.service');
const { DevicesService } = require('../dist/devices/devices.service');
const { PairingService } = require('../dist/pairing/pairing.service');
const { config } = require('../dist/config');

test('AuthService Unit Tests', async (t) => {
  await t.test('generateTokens should return valid access and refresh tokens', () => {
    const authService = new AuthService();
    const userId = 'user-12345';
    const email = 'test@virtualcam.io';

    const tokens = authService.generateTokens(userId, email);

    assert.ok(tokens.accessToken, 'accessToken should be defined');
    assert.ok(tokens.refreshToken, 'refreshToken should be defined');
    assert.equal(tokens.tokenType, 'Bearer');

    // Verify access token
    const decodedAccess = jwt.verify(tokens.accessToken, config.jwt.secret);
    assert.equal(decodedAccess.userId, userId);
    assert.equal(decodedAccess.email, email);

    // Verify refresh token
    const decodedRefresh = jwt.verify(tokens.refreshToken, config.jwt.refreshSecret);
    assert.equal(decodedRefresh.userId, userId);
    assert.equal(decodedRefresh.email, email);
  });
});

test('DevicesService Unit Tests', async (t) => {
  const devicesService = new DevicesService();

  await t.test('generateDeviceUuid should create valid CAMERA UUID', () => {
    const uuid = devicesService.generateDeviceUuid('CAMERA');
    assert.match(uuid, /^VRT-CAM-[0-9A-F]{8}$/);
  });

  await t.test('generateDeviceUuid should create valid VIEWER UUID', () => {
    const uuid = devicesService.generateDeviceUuid('VIEWER');
    assert.match(uuid, /^VRT-VIE-[0-9A-F]{8}$/);
  });

  await t.test('generateDeviceUuid should generate unique IDs on consecutive calls', () => {
    const id1 = devicesService.generateDeviceUuid('CAMERA');
    const id2 = devicesService.generateDeviceUuid('CAMERA');
    assert.notEqual(id1, id2);
  });
});

test('PairingService Unit Tests', async (t) => {
  const pairingService = new PairingService();

  await t.test('generatePairingCode should format as 3 letters, hyphen, 3 digits (e.g. ABC-123)', () => {
    const code = pairingService.generatePairingCode();
    assert.match(code, /^[A-Z]{3}-[0-9]{3}$/);
  });

  await t.test('generatePairingCode should generate distinct codes', () => {
    const code1 = pairingService.generatePairingCode();
    const code2 = pairingService.generatePairingCode();
    assert.notEqual(code1, code2);
  });
});
