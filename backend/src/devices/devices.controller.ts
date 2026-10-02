import { Response, NextFunction } from 'express';
import { z } from 'zod';
import { devicesService } from './devices.service';
import { AuthenticatedRequest } from '../middleware/auth';

const registerDeviceSchema = z.object({
  deviceType: z.enum(['CAMERA', 'VIEWER']),
  deviceName: z.string().optional(),
  deviceUuid: z.string().optional(),
  platform: z.string().optional(),
  appVersion: z.string().optional()
});

const telemetrySchema = z.object({
  batteryLevel: z.number().min(0).max(100).optional(),
  networkType: z.string().optional(),
  streamStatus: z.string().optional(),
  cameraType: z.string().optional(),
  resolution: z.string().optional(),
  fps: z.number().optional(),
  appVersion: z.string().optional()
});

export class DevicesController {
  async register(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = registerDeviceSchema.parse(req.body);
      const device = await devicesService.registerDevice({
        ...validated,
        userId: req.user!.userId
      });

      res.status(201).json({
        success: true,
        data: device
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      next(err);
    }
  }

  async getAll(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const devices = await devicesService.getUserDevices(req.user!.userId);
      res.status(200).json({
        success: true,
        data: devices
      });
    } catch (err: any) {
      next(err);
    }
  }

  async getById(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const device = await devicesService.getDeviceById(req.params.id, req.user!.userId);
      res.status(200).json({
        success: true,
        data: device
      });
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }

  async delete(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const result = await devicesService.deleteDevice(req.params.id, req.user!.userId);
      res.status(200).json(result);
    } catch (err: any) {
      res.status(404).json({ success: false, error: err.message });
    }
  }

  async updateTelemetry(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const validated = telemetrySchema.parse(req.body);
      const result = await devicesService.updateTelemetry(req.params.id, validated);
      res.status(200).json(result);
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ success: false, errors: err.errors });
      }
      next(err);
    }
  }
}

export const devicesController = new DevicesController();
