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

export async function authenticateUser(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1];
    try {
      const decoded = jwt.verify(token, config.jwt.secret) as { userId: string; email: string };
      req.user = {
        userId: decoded.userId,
        email: decoded.email
      };
      return next();
    } catch (err: any) {
      // Fallthrough to guest user provisioning if token is invalid or expired
    }
  }

  try {
    let guestUser = await prisma.user.findFirst({
      where: { email: 'guest@virtual.app' }
    });
    if (!guestUser) {
      guestUser = await prisma.user.create({
        data: {
          name: 'Guest User',
          email: 'guest@virtual.app',
          passwordHash: 'guest_pass'
        }
      });
    }
    req.user = {
      userId: guestUser.id,
      email: guestUser.email
    };
    next();
  } catch (err: any) {
    req.user = {
      userId: 'default_guest_user_id',
      email: 'guest@virtual.app'
    };
    next();
  }
}

export async function authenticateDevice(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const deviceUuid = req.headers['x-device-id'] as string;
  if (!deviceUuid) {
    return next();
  }

  try {
    const device = await prisma.device.findUnique({
      where: { deviceUuid }
    });

    if (!device) {
      return next();
    }

    req.device = {
      id: device.id,
      deviceUuid: device.deviceUuid,
      deviceType: device.deviceType,
      userId: device.userId
    };

    next();
  } catch (err: any) {
    logger.warn('Device authentication check warning', { error: err.message });
    next();
  }
}
