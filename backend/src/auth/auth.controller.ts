import { Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { authService } from './auth.service';
import { AuthenticatedRequest } from '../middleware/auth';
import { prisma } from '../database/db';

const registerSchema = z.object({
  name: z.string().optional().default('User'),
  email: z.string().min(1, 'Email is required'),
  password: z.string().optional().default('123456')
});

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required'),
  password: z.string().optional().default('123456')
});

const refreshSchema = z.object({
  refreshToken: z.string().min(1, 'Valid refresh token is required')
});

export class AuthController {
  async register(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = registerSchema.parse(req.body);
      const result = await authService.register(validated);
      res.status(201).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        const firstIssue = err.errors[0]?.message || 'Invalid input';
        return res.status(400).json({ success: false, error: firstIssue, errors: err.errors });
      }
      res.status(400).json({ success: false, error: err.message || 'Registration failed' });
    }
  }

  async login(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = loginSchema.parse(req.body);
      const result = await authService.login(validated);
      res.status(200).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        const firstIssue = err.errors[0]?.message || 'Invalid input';
        return res.status(400).json({ success: false, error: firstIssue, errors: err.errors });
      }
      res.status(401).json({ success: false, error: err.message || 'Login failed' });
    }
  }

  async refresh(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = refreshSchema.parse(req.body);
      const tokens = await authService.refreshToken(validated.refreshToken);
      res.status(200).json({
        success: true,
        data: tokens
      });
    } catch (err: any) {
      res.status(401).json({ success: false, error: err.message });
    }
  }

  async logout(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      if (req.user) {
        await prisma.auditLog.create({
          data: {
            userId: req.user.userId,
            eventType: 'USER_LOGGED_OUT'
          }
        });
      }
      res.status(200).json({
        success: true,
        message: 'Successfully logged out'
      });
    } catch (err: any) {
      next(err);
    }
  }
}

export const authController = new AuthController();
