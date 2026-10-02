import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { logger } from '../utils/logger';
import { prisma } from '../database/db';

export interface AuthenticatedUser {
  userId: string;
  email: string;
}

export interface AuthenticatedRequest extends Request {
  user?: AuthenticatedUser;
  device?: {
    id: string;
    deviceUuid: string;
    deviceType: string;
    userId: string;
  };
}

export function authenticateUser(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'Authentication required. Missing Bearer token.'
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, config.jwt.secret) as { userId: string; email: string };
    req.user = {
      userId: decoded.userId,
      email: decoded.email
    };
    next();
  } catch (err: any) {
    logger.warn('JWT verification failed', { error: err.message });
    return res.status(401).json({
      success: false,
      error: 'Invalid or expired access token.'
    });
  }
}

export async function authenticateDevice(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const deviceUuid = req.headers['x-device-id'] as string;
  if (!deviceUuid) {
    return res.status(400).json({
      success: false,
      error: 'Missing required X-Device-Id header.'
    });
  }

  try {
    const device = await prisma.device.findUnique({
      where: { deviceUuid }
    });

    if (!device) {
      return res.status(404).json({
        success: false,
        error: `Device ${deviceUuid} not registered.`
      });
    }

    if (req.user && device.userId !== req.user.userId) {
      return res.status(403).json({
        success: false,
        error: 'Unauthorized access to device.'
      });
    }

    req.device = {
      id: device.id,
      deviceUuid: device.deviceUuid,
      deviceType: device.deviceType,
      userId: device.userId
    };

    next();
  } catch (err: any) {
    logger.error('Device authentication error', { error: err.message });
    return res.status(500).json({
      success: false,
      error: 'Failed to authenticate device.'
    });
  }
}
