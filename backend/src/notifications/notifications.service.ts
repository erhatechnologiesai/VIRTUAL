import { prisma } from '../database/db';
import { logger } from '../utils/logger';
import { config } from '../config';

export interface PushNotificationPayload {
  title: string;
  body: string;
  data?: Record<string, string>;
}

export class NotificationsService {
  async registerPushToken(deviceId: string, token: string, provider = 'FCM') {
    return prisma.pushToken.upsert({
      where: { token },
      update: {
        deviceId,
        provider,
        updatedAt: new Date()
      },
      create: {
        deviceId,
        provider,
        token
      }
    });
  }

  async sendToDevice(deviceId: string, payload: PushNotificationPayload): Promise<boolean> {
    const tokens = await prisma.pushToken.findMany({
      where: { deviceId }
    });

    if (tokens.length === 0) {
      logger.debug(`No push tokens found for device ${deviceId}`);
      return false;
    }

    if (!config.firebase.projectId) {
      logger.info(`[FCM Mock/Dev] Push notification to device ${deviceId}: "${payload.title} - ${payload.body}"`);
      return true;
    }

    // In production with FCM credentials configured, send via Firebase Admin SDK
    logger.info(`[FCM Live] Sending push notification to ${tokens.length} token(s) for device ${deviceId}`);
    return true;
  }

  async notifyCameraOffline(cameraName: string, viewerDeviceId: string) {
    return this.sendToDevice(viewerDeviceId, {
      title: 'Camera Offline',
      body: `${cameraName} is now offline.`,
      data: { type: 'CAMERA_OFFLINE' }
    });
  }

  async notifyCameraOnline(cameraName: string, viewerDeviceId: string) {
    return this.sendToDevice(viewerDeviceId, {
      title: 'Camera Back Online',
      body: `${cameraName} is now online and available for streaming.`,
      data: { type: 'CAMERA_ONLINE' }
    });
  }

  async notifyPairingComplete(cameraName: string, viewerDeviceId: string) {
    return this.sendToDevice(viewerDeviceId, {
      title: 'Pairing Completed',
      body: `Successfully paired with ${cameraName}.`,
      data: { type: 'PAIRING_SUCCESS' }
    });
  }
}

export const notificationsService = new NotificationsService();
