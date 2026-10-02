import { DevicesService } from '../src/devices/devices.service';

describe('DevicesService Unit Tests', () => {
  const devicesService = new DevicesService();

  test('generateDeviceUuid should create valid CAMERA UUID', () => {
    const uuid = devicesService.generateDeviceUuid('CAMERA');
    expect(uuid).toMatch(/^VRT-CAM-[0-9A-F]{8}$/);
  });

  test('generateDeviceUuid should create valid VIEWER UUID', () => {
    const uuid = devicesService.generateDeviceUuid('VIEWER');
    expect(uuid).toMatch(/^VRT-VIE-[0-9A-F]{8}$/);
  });

  test('generateDeviceUuid should generate unique IDs on consecutive calls', () => {
    const id1 = devicesService.generateDeviceUuid('CAMERA');
    const id2 = devicesService.generateDeviceUuid('CAMERA');
    expect(id1).not.toBe(id2);
  });
});
