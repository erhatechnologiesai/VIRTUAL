import { Router } from 'express';
import { authController } from './auth.controller';
import { authenticateUser } from '../middleware/auth';

export const authRouter = Router();

authRouter.post('/register', (req, res, next) => authController.register(req, res, next));
authRouter.post('/login', (req, res, next) => authController.login(req, res, next));
authRouter.post('/refresh', (req, res, next) => authController.refresh(req, res, next));
authRouter.post('/logout', authenticateUser, (req, res, next) => authController.logout(req, res, next));
