import { prisma } from '../database/db';
import { logger } from '../utils/logger';
import crypto from 'crypto';

export interface RegisterDeviceDto {
  userId: string;
  deviceType: 'CAMERA' | 'VIEWER';
  deviceName?: string;
  deviceUuid?: string;
  platform?: string;
  appVersion?: string;
}

export interface UpdateTelemetryDto {
  batteryLevel?: number;
  networkType?: string;
  streamStatus?: string;
  cameraType?: string;
  resolution?: string;
  fps?: number;
  appVersion?: string;
}

export class DevicesService {
  /**
   * Generates a standard VIRTUAL Device UUID: VRT-CAM-XXXXXXXX or VRT-VIE-XXXXXXXX
   */
  generateDeviceUuid(deviceType: 'CAMERA' | 'VIEWER'): string {
    const prefix = deviceType === 'CAMERA' ? 'VRT-CAM-' : 'VRT-VIE-';
    const randomHex = crypto.randomBytes(4).toString('hex').toUpperCase();
    return `${prefix}${randomHex}`;
  }

  async registerDevice(data: RegisterDeviceDto) {
    const deviceUuid = data.deviceUuid || this.generateDeviceUuid(data.deviceType);
    const defaultName = data.deviceName || (data.deviceType === 'CAMERA' ? 'Camera Device' : 'Viewer Device');

    // Check if device already exists (e.g. re-registering on same hardware)
    let device = await prisma.device.findUnique({
      where: { deviceUuid },
      include: { cameraDetail: true, settings: true }
    });

    if (device) {
      // Update existing device
      device = await prisma.device.update({
        where: { id: device.id },
        data: {
          deviceName: defaultName,
          appVersion: data.appVersion || device.appVersion,
          status: 'ONLINE',
          lastSeen: new Date(),
          userId: data.userId
        },
        include: { cameraDetail: true, settings: true }
      });
      logger.info(`Existing device re-registered: ${device.deviceUuid}`);
      return device;
    }

    // Create new device transactionally with settings and cameraDetail if CAMERA
    device = await prisma.device.create({
      data: {
        userId: data.userId,
        deviceUuid,
        deviceName: defaultName,
        deviceType: data.deviceType,
        platform: data.platform || 'ANDROID',
        appVersion: data.appVersion || '1.0.0',
        status: 'ONLINE',
        lastSeen: new Date(),
        settings: {
          create: {
            videoQuality: '720p',
            fps: 30,
            audioEnabled: false,
            autoReconnect: true
          }
        },
        ...(data.deviceType === 'CAMERA'
          ? {
              cameraDetail: {
                create: {
                  cameraType: 'BACK',
                  resolution: '720p',
                  fps: 30,
                  audioEnabled: false,
                  streamStatus: 'IDLE',
                  batteryLevel: 100,
                  networkType: 'WIFI'
                }
              }
            }
          : {})
      },
      include: {
        cameraDetail: true,
        settings: true
      }
    });

    await prisma.auditLog.create({
      data: {
        userId: data.userId,
        deviceId: device.id,
        eventType: 'DEVICE_REGISTERED',
        metadata: { deviceUuid, deviceType: data.deviceType }
      }
    });

    logger.info(`New device registered: ${device.deviceUuid} (${device.deviceType})`);
    return device;
  }

  async getUserDevices(userId: string) {
    return prisma.device.findMany({
      where: { userId },
      include: {
        cameraDetail: true,
        settings: true,
        asCameraPairings: {
          where: { pairingStatus: 'ACTIVE' },
          include: { viewerDevice: true }
        },
        asViewerPairings: {
          where: { pairingStatus: 'ACTIVE' },
          include: { cameraDevice: { include: { cameraDetail: true } } }
        }
      },
      orderBy: { createdAt: 'desc' }
    });
  }

  async getDeviceById(id: string, userId: string) {
    const device = await prisma.device.findFirst({
      where: { id, userId },
      include: {
        cameraDetail: true,
        settings: true,
        asCameraPairings: {
          where: { pairingStatus: 'ACTIVE' },
          include: { viewerDevice: true }
        },
        asViewerPairings: {
          where: { pairingStatus: 'ACTIVE' },
          include: { cameraDevice: { include: { cameraDetail: true } } }
        }
      }
    });

    if (!device) {
      throw new Error('Device not found or unauthorized');
    }

    return device;
  }

  async deleteDevice(id: string, userId: string) {
    const device = await prisma.device.findFirst({
      where: { id, userId }
    });

    if (!device) {
      throw new Error('Device not found or unauthorized');
    }

    // Revoke any active pairings first
    await prisma.pairing.updateMany({
      where: {
        OR: [{ cameraDeviceId: id }, { viewerDeviceId: id }],
        pairingStatus: 'ACTIVE'
      },
      data: {
        pairingStatus: 'REVOKED',
        revokedAt: new Date()
      }
    });

    await prisma.device.delete({
      where: { id }
    });

    await prisma.auditLog.create({
      data: {
        userId,
        eventType: 'DEVICE_REMOVED',
        metadata: { deviceUuid: device.deviceUuid }
      }
    });

    logger.info(`Device deleted: ${device.deviceUuid}`);
    return { success: true, message: 'Device removed successfully' };
  }

  async updateTelemetry(deviceId: string, data: UpdateTelemetryDto) {
    const updateData: any = {
      lastSeen: new Date(),
      status: data.streamStatus === 'STREAMING' ? 'STREAMING' : 'ONLINE'
    };

    if (data.appVersion) {
      updateData.appVersion = data.appVersion;
    }

    await prisma.device.update({
      where: { id: deviceId },
      data: updateData
    });

    // If camera device, update cameraDetail
    const cameraDetail = await prisma.cameraDevice.findUnique({
      where: { deviceId }
    });

    if (cameraDetail) {
      const cameraUpdates: any = {};
      if (data.batteryLevel !== undefined) cameraUpdates.batteryLevel = data.batteryLevel;
      if (data.networkType !== undefined) cameraUpdates.networkType = data.networkType;
      if (data.streamStatus !== undefined) cameraUpdates.streamStatus = data.streamStatus;
      if (data.cameraType !== undefined) cameraUpdates.cameraType = data.cameraType;
      if (data.resolution !== undefined) cameraUpdates.resolution = data.resolution;
      if (data.fps !== undefined) cameraUpdates.fps = data.fps;

      await prisma.cameraDevice.update({
        where: { deviceId },
        data: cameraUpdates
      });
    }

    return { success: true, timestamp: new Date() };
  }
}

export const devicesService = new DevicesService();
