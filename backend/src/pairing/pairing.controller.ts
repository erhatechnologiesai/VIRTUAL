import { Response, NextFunction } from 'express';
import { z } from 'zod';
import { pairingService } from './pairing.service';
import { AuthenticatedRequest } from '../middleware/auth';
import { signalingServer } from '../signaling/signaling.server';

const createPairingSchema = z.object({
  cameraDeviceId: z.string().uuid('Valid camera device ID is required')
});

const joinPairingSchema = z.object({
  viewerDeviceId: z.string().uuid('Valid viewer device ID is required'),
  pairingToken: z.string().optional(),
  pairingCode: z.string().optional()
}).refine(data => data.pairingToken || data.pairingCode, {
  message: 'Either pairingToken or pairingCode must be provided'
});

export class PairingController {
  async create(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = createPairingSchema.parse(req.body);
      const result = await pairingService.createPairingSession({
        cameraDeviceId: validated.cameraDeviceId,
        userId: req.user!.userId
      });

      res.status(201).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      res.status(400).json({ success: false, error: err.message });
    }
  }

  async join(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = joinPairingSchema.parse(req.body);
      const pairing = await pairingService.joinPairing({
        ...validated,
        userId: req.user!.userId
      });

      // Notify camera device via WebRTC signaling socket that pairing was successful!
      if (signalingServer) {
        signalingServer.notifyPairingSuccess(pairing.cameraDeviceId, pairing.viewerDeviceId, {
          pairingId: pairing.id,
          cameraName: pairing.cameraDevice.deviceName,
          viewerName: pairing.viewerDevice.deviceName
        });
      }

      res.status(200).json({
        success: true,
        data: pairing
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      res.status(400).json({ success: false, error: err.message });
    }
  }

  async getPairings(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const pairings = await pairingService.getPairingsForUser(req.user!.userId);
      res.status(200).json({
        success: true,
        data: pairings
      });
    } catch (err: any) {
      next(err);
    }
  }

  async revoke(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const result = await pairingService.revokePairing(req.params.id, req.user!.userId);

      // Notify devices via signaling server
      if (signalingServer) {
        signalingServer.notifyPairingRevoked(result.cameraDeviceId, result.viewerDeviceId, result.id);
      }

      res.status(200).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }
}

export const pairingController = new PairingController();
