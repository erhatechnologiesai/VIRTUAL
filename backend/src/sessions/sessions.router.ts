import { Router } from 'express';
import { sessionsController } from './sessions.controller';
import { authenticateUser } from '../middleware/auth';

export const sessionsRouter = Router();

sessionsRouter.use(authenticateUser);

// Camera endpoints
sessionsRouter.get('/cameras', (req, res, next) => sessionsController.getCameras(req, res, next));
sessionsRouter.get('/cameras/:id', (req, res, next) => sessionsController.getCameraById(req, res, next));
sessionsRouter.get('/cameras/:id/status', (req, res, next) => sessionsController.getCameraStatus(req, res, next));

// WebRTC Session tracking endpoints
sessionsRouter.post('/sessions', (req, res, next) => sessionsController.startSession(req, res, next));
sessionsRouter.delete('/sessions/:id', (req, res, next) => sessionsController.endSession(req, res, next));

// Settings endpoints
sessionsRouter.get('/settings', (req, res, next) => sessionsController.getSettings(req, res, next));
sessionsRouter.put('/settings', (req, res, next) => sessionsController.updateSettings(req, res, next));

// Dynamic STUN/TURN ICE server configuration
sessionsRouter.get('/webrtc/ice-servers', (req, res) => sessionsController.getIceServers(req, res));
