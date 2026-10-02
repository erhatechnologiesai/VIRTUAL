import { Router } from 'express';
import { devicesController } from './devices.controller';
import { authenticateUser } from '../middleware/auth';

export const devicesRouter = Router();

devicesRouter.use(authenticateUser);

devicesRouter.post('/', (req, res, next) => devicesController.register(req, res, next));
devicesRouter.get('/', (req, res, next) => devicesController.getAll(req, res, next));
devicesRouter.get('/:id', (req, res, next) => devicesController.getById(req, res, next));
devicesRouter.delete('/:id', (req, res, next) => devicesController.delete(req, res, next));
devicesRouter.post('/:id/telemetry', (req, res, next) => devicesController.updateTelemetry(req, res, next));
