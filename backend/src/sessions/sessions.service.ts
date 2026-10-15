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
    // Resolve camera and viewer devices by id OR deviceUuid
    const camera = await prisma.device.findFirst({
      where: {
        OR: [
          { id: data.cameraDeviceId },
          { deviceUuid: data.cameraDeviceId }
        ]
      }
    });

    const viewer = await prisma.device.findFirst({
      where: {
        OR: [
          { id: data.viewerDeviceId },
          { deviceUuid: data.viewerDeviceId }
        ]
      }
    });

    const camId = camera ? camera.id : data.cameraDeviceId;
    const viewId = viewer ? viewer.id : data.viewerDeviceId;

    // Check pairing
    let pairing = await prisma.pairing.findFirst({
      where: {
        OR: [
          { cameraDeviceId: camId, viewerDeviceId: viewId, pairingStatus: 'ACTIVE' },
          { cameraDeviceId: data.cameraDeviceId, viewerDeviceId: data.viewerDeviceId, pairingStatus: 'ACTIVE' }
        ]
      }
    });

    // If both devices exist and share user or are paired, auto-ensure pairing
    if (!pairing && camera && viewer && camera.userId === viewer.userId) {
      pairing = await prisma.pairing.upsert({
        where: {
          cameraDeviceId_viewerDeviceId: {
            cameraDeviceId: camera.id,
            viewerDeviceId: viewer.id
          }
        },
        update: {
          pairingStatus: 'ACTIVE',
          pairedAt: new Date()
        },
        create: {
          cameraDeviceId: camera.id,
          viewerDeviceId: viewer.id,
          pairingStatus: 'ACTIVE',
          pairedAt: new Date()
        }
      });
    }

    // End any existing active sessions between these devices
    if (camera && viewer) {
      await prisma.session.updateMany({
        where: {
          cameraDeviceId: camera.id,
          viewerDeviceId: viewer.id,
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
          cameraDeviceId: camera.id,
          viewerDeviceId: viewer.id,
          status: 'ACTIVE',
          startedAt: new Date()
        }
      });

      // Update camera stream status
      await prisma.cameraDevice.updateMany({
        where: { deviceId: camera.id },
        data: {
          streamStatus: 'STREAMING',
          lastStreamStarted: new Date()
        }
      });

      await prisma.device.update({
        where: { id: camera.id },
        data: { status: 'STREAMING', lastSeen: new Date() }
      });

      logger.info(`WebRTC Session started: Session [${session.id}] Camera [${data.cameraDeviceId}] <-> Viewer [${data.viewerDeviceId}]`);
      return session;
    } else {
      const mockSession = {
        id: `sess_${Date.now()}`,
        cameraDeviceId: data.cameraDeviceId,
        viewerDeviceId: data.viewerDeviceId,
        status: 'ACTIVE',
        startedAt: new Date()
      };
      logger.info(`WebRTC Direct Session active: Camera [${data.cameraDeviceId}] <-> Viewer [${data.viewerDeviceId}]`);
      return mockSession;
    }
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
    const servers: any[] = [
      {
        urls: [
          'stun:stun.l.google.com:19302',
          'stun:stun1.l.google.com:19302',
          'stun:stun2.l.google.com:19302',
          'stun:stun3.l.google.com:19302',
          'stun:stun4.l.google.com:19302',
          'stun:stun.cloudflare.com:3478'
        ]
      }
    ];

    if (config.webrtc.turnUrl && config.webrtc.turnUrl.trim() !== '' && !config.webrtc.turnUrl.includes('virtualcam.io')) {
      servers.push({
        urls: [config.webrtc.turnUrl],
        username: config.webrtc.turnUsername,
        credential: config.webrtc.turnPassword
      });
    }

    return servers;
  }
}

export const sessionsService = new SessionsService();
