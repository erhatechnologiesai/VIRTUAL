import express, { Express, Request, Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { config } from './config';
import { authRouter } from './auth/auth.router';
import { devicesRouter } from './devices/devices.router';
import { pairingRouter } from './pairing/pairing.router';
import { sessionsRouter } from './sessions/sessions.router';
import { errorHandler } from './middleware/errorHandler';
import { authenticateUser, AuthenticatedRequest } from './middleware/auth';
import { notificationsService } from './notifications/notifications.service';

export function createApp(): Express {
  const app = express();

  // Trust proxy for tunnels and reverse proxies
  app.set('trust proxy', true);

  // Security headers
  app.use(helmet());

  // CORS configuration
  app.use(
    cors({
      origin: config.corsOrigin,
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
    })
  );

  // Rate Limiting to prevent brute-force and DDoS
  const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 500, // limit each IP to 500 requests per window
    standardHeaders: true,
    legacyHeaders: false,
    message: {
      success: false,
      error: 'Too many requests from this IP, please try again after 15 minutes'
    }
  });

  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 50, // limit auth attempts
    standardHeaders: true,
    legacyHeaders: false,
    message: {
      success: false,
      error: 'Too many authentication attempts, please try again later'
    }
  });

  app.use('/api/', apiLimiter);
  app.use('/api/auth/login', authLimiter);
  app.use('/api/auth/register', authLimiter);

  // Body parser
  app.use(express.json({ limit: '1mb' }));
  app.use(express.urlencoded({ extended: true }));

  // Health check endpoints (unauthenticated)
  const handleHealth = (_req: Request, res: Response) => {
    res.status(200).json({
      status: 'healthy',
      ok: true,
      service: 'virtual-backend',
      timestamp: new Date().toISOString()
    });
  };

  app.get('/health', handleHealth);
  app.get('/api/health', handleHealth);

  // REST API Routes
  app.use('/api/auth', authRouter);
  app.use('/api/devices', devicesRouter);
  app.use('/api/pairing', pairingRouter);
  app.use('/api', sessionsRouter);

  // Push notification token registration endpoint
  app.post('/api/notifications/token', authenticateUser, async (req: AuthenticatedRequest, res: Response, next) => {
    try {
      const { deviceId, token, provider } = req.body;
      if (!deviceId || !token) {
        return res.status(400).json({ success: false, error: 'deviceId and token are required' });
      }
      await notificationsService.registerPushToken(deviceId, token, provider);
      res.status(200).json({ success: true, message: 'Push token registered successfully' });
    } catch (err) {
      next(err);
    }
  });

  // 404 Handler
  app.use((req: Request, res: Response) => {
    res.status(404).json({
      success: false,
      error: `Cannot ${req.method} ${req.originalUrl}`
    });
  });

  // Error Handler
  app.use(errorHandler);

  return app;
}
