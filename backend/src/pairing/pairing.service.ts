import crypto from 'crypto';
import { prisma } from '../database/db';
import { logger } from '../utils/logger';

export interface CreatePairingDto {
  cameraDeviceId: string;
  userId: string;
}

export interface JoinPairingDto {
  viewerDeviceId: string;
  userId: string;
  pairingToken?: string;
  pairingCode?: string;
}

export class PairingService {
  /**
   * Generates a 6-character pairing code: e.g. "ABC-123"
   */
  generatePairingCode(): string {
    const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const numbers = '23456789';
    let codeLetters = '';
    let codeNumbers = '';
    for (let i = 0; i < 3; i++) {
      codeLetters += letters.charAt(Math.floor(Math.random() * letters.length));
      codeNumbers += numbers.charAt(Math.floor(Math.random() * numbers.length));
    }
    return `${codeLetters}-${codeNumbers}`;
  }

  async createPairingSession(data: CreatePairingDto) {
    // Verify camera device belongs to user
    const camera = await prisma.device.findFirst({
      where: { id: data.cameraDeviceId, userId: data.userId, deviceType: 'CAMERA' }
    });

    if (!camera) {
      throw new Error('Camera device not found or unauthorized');
    }

    // Generate short-lived pairing secret (10 minutes)
    const rawToken = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
    const pairingCode = this.generatePairingCode();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes

    // Delete any prior expired or pending pairings for this camera
    await prisma.pairing.deleteMany({
      where: {
        cameraDeviceId: camera.id,
        pairingStatus: 'PENDING'
      }
    });

    // Create pending pairing
    const pairing = await prisma.pairing.create({
      data: {
        cameraDeviceId: camera.id,
        viewerDeviceId: camera.id, // temporary placeholder until viewer claims
        pairingStatus: 'PENDING',
        pairingTokenHash: tokenHash,
        pairingCode,
        expiresAt
      }
    });

    const qrPayload = JSON.stringify({
      v: '1.0',
      action: 'VIRTUAL_PAIR',
      cameraId: camera.id,
      deviceUuid: camera.deviceUuid,
      token: rawToken,
      code: pairingCode,
      exp: expiresAt.toISOString()
    });

    logger.info(`Pairing session created for Camera ${camera.deviceUuid}, code: ${pairingCode}`);

    return {
      pairingId: pairing.id,
      cameraDeviceId: camera.id,
      cameraUuid: camera.deviceUuid,
      pairingCode,
      pairingToken: rawToken,
      qrData: qrPayload,
      expiresAt,
      expiresInSeconds: 600
    };
  }

  async joinPairing(data: JoinPairingDto) {
    const viewer = await prisma.device.findFirst({
      where: { id: data.viewerDeviceId, userId: data.userId, deviceType: 'VIEWER' }
    });

    if (!viewer) {
      throw new Error('Viewer device not found or unauthorized');
    }

    let pairing = null;

    if (data.pairingToken) {
      const tokenHash = crypto.createHash('sha256').update(data.pairingToken).digest('hex');
      pairing = await prisma.pairing.findFirst({
        where: {
          pairingTokenHash: tokenHash,
          pairingStatus: 'PENDING',
          expiresAt: { gt: new Date() }
        },
        include: { cameraDevice: { include: { cameraDetail: true } } }
      });
    } else if (data.pairingCode) {
      const formattedCode = data.pairingCode.trim().toUpperCase();
      pairing = await prisma.pairing.findFirst({
        where: {
          pairingCode: formattedCode,
          pairingStatus: 'PENDING',
          expiresAt: { gt: new Date() }
        },
        include: { cameraDevice: { include: { cameraDetail: true } } }
      });
    }

    if (!pairing) {
      throw new Error('Invalid or expired pairing code/token. Please generate a new QR code.');
    }

    // Check if pairing between these two already exists
    const existingPairing = await prisma.pairing.findFirst({
      where: {
        cameraDeviceId: pairing.cameraDeviceId,
        viewerDeviceId: viewer.id
      }
    });

    let activatedPairing;

    if (existingPairing) {
      // Re-activate existing pairing
      activatedPairing = await prisma.pairing.update({
        where: { id: existingPairing.id },
        data: {
          pairingStatus: 'ACTIVE',
          pairedAt: new Date(),
          revokedAt: null,
          pairingTokenHash: null,
          pairingCode: null
        },
        include: {
          cameraDevice: { include: { cameraDetail: true } },
          viewerDevice: true
        }
      });
      // Delete temporary pending pairing
      await prisma.pairing.delete({ where: { id: pairing.id } });
    } else {
      // Finalize this pairing record
      activatedPairing = await prisma.pairing.update({
        where: { id: pairing.id },
        data: {
          viewerDeviceId: viewer.id,
          pairingStatus: 'ACTIVE',
          pairedAt: new Date(),
          pairingTokenHash: null,
          pairingCode: null
        },
        include: {
          cameraDevice: { include: { cameraDetail: true } },
          viewerDevice: true
        }
      });
    }

    // Record audit log
    await prisma.auditLog.create({
      data: {
        userId: data.userId,
        deviceId: viewer.id,
        eventType: 'DEVICES_PAIRED',
        metadata: {
          cameraId: activatedPairing.cameraDeviceId,
          cameraUuid: activatedPairing.cameraDevice.deviceUuid,
          viewerId: viewer.id,
          viewerUuid: viewer.deviceUuid
        }
      }
    });

    logger.info(`Device paired successfully: Camera [${activatedPairing.cameraDevice.deviceUuid}] <-> Viewer [${viewer.deviceUuid}]`);

    return activatedPairing;
  }

  async getPairingsForUser(userId: string) {
    return prisma.pairing.findMany({
      where: {
        pairingStatus: 'ACTIVE',
        OR: [
          { cameraDevice: { userId } },
          { viewerDevice: { userId } }
        ]
      },
      include: {
        cameraDevice: { include: { cameraDetail: true } },
        viewerDevice: true
      },
      orderBy: { pairedAt: 'desc' }
    });
  }

  async revokePairing(pairingId: string, userId: string) {
    const pairing = await prisma.pairing.findFirst({
      where: {
        id: pairingId,
        OR: [
          { cameraDevice: { userId } },
          { viewerDevice: { userId } }
        ]
      },
      include: { cameraDevice: true, viewerDevice: true }
    });

    if (!pairing) {
      throw new Error('Pairing not found or unauthorized');
    }

    const updated = await prisma.pairing.update({
      where: { id: pairingId },
      data: {
        pairingStatus: 'REVOKED',
        revokedAt: new Date()
      }
    });

    await prisma.auditLog.create({
      data: {
        userId,
        eventType: 'PAIRING_REVOKED',
        metadata: {
          pairingId,
          cameraUuid: pairing.cameraDevice.deviceUuid,
          viewerUuid: pairing.viewerDevice.deviceUuid
        }
      }
    });

    logger.info(`Pairing revoked: Camera [${pairing.cameraDevice.deviceUuid}] <-> Viewer [${pairing.viewerDevice.deviceUuid}]`);

    return updated;
  }

  async isPairingAuthorized(cameraDeviceId: string, viewerDeviceId: string): Promise<boolean> {
    const pairing = await prisma.pairing.findFirst({
      where: {
        cameraDeviceId,
        viewerDeviceId,
        pairingStatus: 'ACTIVE'
      }
    });
    return !!pairing;
  }
}

export const pairingService = new PairingService();
