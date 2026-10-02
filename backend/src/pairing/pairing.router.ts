import { Router } from 'express';
import { pairingController } from './pairing.controller';
import { authenticateUser } from '../middleware/auth';

export const pairingRouter = Router();

pairingRouter.use(authenticateUser);

pairingRouter.post('/create', (req, res, next) => pairingController.create(req, res, next));
pairingRouter.post('/join', (req, res, next) => pairingController.join(req, res, next));
pairingRouter.get('/', (req, res, next) => pairingController.getPairings(req, res, next));
pairingRouter.delete('/:id', (req, res, next) => pairingController.revoke(req, res, next));
