import { prisma } from '../database/db';
import { logger } from '../utils/logger';
import { config } from '../config';

export interface StartSessionDto {
  cameraDeviceId: string;
  viewerDeviceId: string;
}

export interface EndSessionDto {
  sessionId: string;
  disconnectReason?: string;
}

export interface UpdateSettingsDto {
  deviceId: string;
  videoQuality?: string;
  fps?: number;
  audioEnabled?: boolean;
  autoReconnect?: boolean;
}

export class SessionsService {
  async getPairedCamerasForViewer(userId: string) {
    // Find all active pairings where viewer belongs to this user or camera belongs to this user
    const pairings = await prisma.pairing.findMany({
      where: {
        pairingStatus: 'ACTIVE',
        OR: [
          { viewerDevice: { userId } },
          { cameraDevice: { userId } }
        ]
      },
      include: {
        cameraDevice: {
          include: {
            cameraDetail: true,
            settings: true
          }
        },
        viewerDevice: true
      }
    });

    // Map to distinct camera devices
    const cameraMap = new Map<string, any>();
    for (const p of pairings) {
      if (p.cameraDevice && !cameraMap.has(p.cameraDevice.id)) {
        cameraMap.set(p.cameraDevice.id, {
          id: p.cameraDevice.id,
          deviceUuid: p.cameraDevice.deviceUuid,
          deviceName: p.cameraDevice.deviceName,
          status: p.cameraDevice.status,
          lastSeen: p.cameraDevice.lastSeen,
          cameraDetail: p.cameraDevice.cameraDetail,
          settings: p.cameraDevice.settings,
          pairingId: p.id,
          pairedAt: p.pairedAt
        });
      }
    }

    return Array.from(cameraMap.values());
  }

  async getCameraById(cameraId: string, userId: string) {
    const camera = await prisma.device.findFirst({
      where: {
        id: cameraId,
        deviceType: 'CAMERA',
        OR: [
          { userId },
          { asCameraPairings: { some: { viewerDevice: { userId }, pairingStatus: 'ACTIVE' } } }
        ]
      },
      include: {
        cameraDetail: true,
        settings: true
      }
    });

    if (!camera) {
      throw new Error('Camera not found or unauthorized');
    }

    return camera;
  }

  async getCameraStatus(cameraId: string) {
    const camera = await prisma.device.findUnique({
      where: { id: cameraId },
      include: { cameraDetail: true }
    });

    if (!camera) {
      throw new Error('Camera not found');
    }

    // Check if offline based on lastSeen (if > 60 seconds without ping, considered offline)
    const timeDiffSeconds = (Date.now() - new Date(camera.lastSeen).getTime()) / 1000;
    const isOnline = timeDiffSeconds < 60 && camera.status !== 'OFFLINE';

    return {
      cameraId: camera.id,
      deviceUuid: camera.deviceUuid,
      deviceName: camera.deviceName,
      status: isOnline ? camera.status : 'OFFLINE',
      isOnline,
      lastSeen: camera.lastSeen,
      batteryLevel: camera.cameraDetail?.batteryLevel ?? null,
      networkType: camera.cameraDetail?.networkType ?? 'WIFI',
      streamStatus: camera.cameraDetail?.streamStatus ?? 'IDLE',
      resolution: camera.cameraDetail?.resolution ?? '720p',
      fps: camera.cameraDetail?.fps ?? 30
    };
  }

  async startSession(data: StartSessionDto) {
    // Check pairing
    const pairing = await prisma.pairing.findFirst({
      where: {
        cameraDeviceId: data.cameraDeviceId,
        viewerDeviceId: data.viewerDeviceId,
        pairingStatus: 'ACTIVE'
      }
    });

    if (!pairing) {
      throw new Error('Unauthorized: Devices are not paired or pairing has been revoked');
    }

    // End any existing active sessions between these devices
    await prisma.session.updateMany({
      where: {
        cameraDeviceId: data.cameraDeviceId,
        viewerDeviceId: data.viewerDeviceId,
        status: 'ACTIVE'
      },
      data: {
        status: 'COMPLETED',
        endedAt: new Date(),
        disconnectReason: 'SUPERSEDED_BY_NEW_SESSION'
      }
    });

    const session = await prisma.session.create({
      data: {
        cameraDeviceId: data.cameraDeviceId,
        viewerDeviceId: data.viewerDeviceId,
        status: 'ACTIVE',
        startedAt: new Date()
      }
    });

    // Update camera stream status
    await prisma.cameraDevice.updateMany({
      where: { deviceId: data.cameraDeviceId },
      data: {
        streamStatus: 'STREAMING',
        lastStreamStarted: new Date()
      }
    });

    await prisma.device.update({
      where: { id: data.cameraDeviceId },
      data: { status: 'STREAMING' }
    });

    logger.info(`WebRTC Session started: Session [${session.id}] Camera [${data.cameraDeviceId}] <-> Viewer [${data.viewerDeviceId}]`);

    return session;
  }

  async endSession(data: EndSessionDto) {
    const session = await prisma.session.findUnique({
      where: { id: data.sessionId }
    });

    if (!session) {
      throw new Error('Session not found');
    }

    const updated = await prisma.session.update({
      where: { id: data.sessionId },
      data: {
        status: 'COMPLETED',
        endedAt: new Date(),
        disconnectReason: data.disconnectReason || 'NORMAL_CLOSURE'
      }
    });

    // Check if camera has other active sessions
    const otherActiveSessions = await prisma.session.count({
      where: {
        cameraDeviceId: session.cameraDeviceId,
        status: 'ACTIVE',
        id: { not: session.id }
      }
    });

    if (otherActiveSessions === 0) {
      await prisma.cameraDevice.updateMany({
        where: { deviceId: session.cameraDeviceId },
        data: {
          streamStatus: 'IDLE',
          lastStreamEnded: new Date()
        }
      });

      await prisma.device.update({
        where: { id: session.cameraDeviceId },
        data: { status: 'ONLINE' }
      });
    }

    logger.info(`WebRTC Session ended: Session [${session.id}] Reason: ${data.disconnectReason}`);
    return updated;
  }

  async getSettings(deviceId: string) {
    let settings = await prisma.deviceSetting.findUnique({
      where: { deviceId }
    });

    if (!settings) {
      settings = await prisma.deviceSetting.create({
        data: {
          deviceId,
          videoQuality: '720p',
          fps: 30,
          audioEnabled: false,
          autoReconnect: true
        }
      });
    }

    return settings;
  }

  async updateSettings(data: UpdateSettingsDto) {
    const settings = await prisma.deviceSetting.upsert({
      where: { deviceId: data.deviceId },
      update: {
        ...(data.videoQuality ? { videoQuality: data.videoQuality } : {}),
        ...(data.fps ? { fps: data.fps } : {}),
        ...(data.audioEnabled !== undefined ? { audioEnabled: data.audioEnabled } : {}),
        ...(data.autoReconnect !== undefined ? { autoReconnect: data.autoReconnect } : {})
      },
      create: {
        deviceId: data.deviceId,
        videoQuality: data.videoQuality || '720p',
        fps: data.fps || 30,
        audioEnabled: data.audioEnabled ?? false,
        autoReconnect: data.autoReconnect ?? true
      }
    });

    // Also update cameraDetail resolution and fps if camera device
    if (data.videoQuality || data.fps || data.audioEnabled !== undefined) {
      await prisma.cameraDevice.updateMany({
        where: { deviceId: data.deviceId },
        data: {
          ...(data.videoQuality ? { resolution: data.videoQuality } : {}),
          ...(data.fps ? { fps: data.fps } : {}),
          ...(data.audioEnabled !== undefined ? { audioEnabled: data.audioEnabled } : {})
        }
      });
    }

    return settings;
  }

  getIceServers() {
    return [
      {
        urls: [config.webrtc.stunUrl]
      },
      {
        urls: [config.webrtc.turnUrl],
        username: config.webrtc.turnUsername,
        credential: config.webrtc.turnPassword
      }
    ];
  }
}

export const sessionsService = new SessionsService();
