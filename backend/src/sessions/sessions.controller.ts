import { Response, NextFunction } from 'express';
import { z } from 'zod';
import { sessionsService } from './sessions.service';
import { AuthenticatedRequest } from '../middleware/auth';

const startSessionSchema = z.object({
  cameraDeviceId: z.string().min(1, 'cameraDeviceId is required'),
  viewerDeviceId: z.string().min(1, 'viewerDeviceId is required')
});

const endSessionSchema = z.object({
  disconnectReason: z.string().optional()
});

const updateSettingsSchema = z.object({
  videoQuality: z.enum(['360p', '480p', '720p', '1080p']).optional(),
  fps: z.number().int().min(10).max(60).optional(),
  audioEnabled: z.boolean().optional(),
  autoReconnect: z.boolean().optional()
});

export class SessionsController {
  async getCameras(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const cameras = await sessionsService.getPairedCamerasForViewer(req.user!.userId);
      res.status(200).json({
        success: true,
        data: cameras
      });
    } catch (err: any) {
      next(err);
    }
  }

  async getCameraById(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const camera = await sessionsService.getCameraById(req.params.id, req.user!.userId);
      res.status(200).json({
        success: true,
        data: camera
      });
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }

  async getCameraStatus(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const status = await sessionsService.getCameraStatus(req.params.id);
      res.status(200).json({
        success: true,
        data: status
      });
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }

  async startSession(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = startSessionSchema.parse(req.body);
      const session = await sessionsService.startSession(validated);
      res.status(201).json({
        success: true,
        data: session
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      res.status(400).json({ success: false, error: err.message });
    }
  }

  async endSession(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = endSessionSchema.parse(req.body);
      const result = await sessionsService.endSession({
        sessionId: req.params.id,
        disconnectReason: validated.disconnectReason
      });
      res.status(200).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }

  async getSettings(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const deviceId = req.query.deviceId as string || req.device?.id;
      if (!deviceId) {
        return res.status(400).json({ success: false, error: 'deviceId is required' });
      }
      const settings = await sessionsService.getSettings(deviceId);
      res.status(200).json({
        success: true,
        data: settings
      });
    } catch (err: any) {
      next(err);
    }
  }

  async updateSettings(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = updateSettingsSchema.parse(req.body);
      const deviceId = req.query.deviceId as string || req.device?.id;
      if (!deviceId) {
        return res.status(400).json({ success: false, error: 'deviceId is required' });
      }
      const settings = await sessionsService.updateSettings({
        deviceId,
        ...validated
      });
      res.status(200).json({
        success: true,
        data: settings
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      next(err);
    }
  }

  getIceServers(req: AuthenticatedRequest, res: Response) {
    const iceServers = sessionsService.getIceServers();
    res.status(200).json({
      success: true,
      data: {
        iceServers
      }
    });
  }
}

export const sessionsController = new SessionsController();
