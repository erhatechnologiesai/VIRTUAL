import { Server as HttpServer } from 'http';
import { Server, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { logger } from '../utils/logger';
import { prisma } from '../database/db';
import { pairingService } from '../pairing/pairing.service';

interface AuthenticatedSocket extends Socket {
  userId?: string;
  deviceId?: string;
  deviceType?: 'CAMERA' | 'VIEWER';
}

export class SignalingServer {
  private io: Server | null = null;
  // Map of deviceId -> Set of socket IDs
  private connectedDevices = new Map<string, Set<string>>();

  init(httpServer: HttpServer) {
    this.io = new Server(httpServer, {
      cors: {
        origin: config.corsOrigin,
        methods: ['GET', 'POST'],
        credentials: true
      },
      pingTimeout: 20000,
      pingInterval: 10000
    });

    // Authentication middleware for Socket.IO
    this.io.use(async (socket: AuthenticatedSocket, next) => {
      try {
        const token = socket.handshake.auth?.token || socket.handshake.headers?.authorization?.replace('Bearer ', '');
        const deviceId = socket.handshake.auth?.deviceId || socket.handshake.query?.deviceId;

        if (!token) {
          logger.warn(`Signaling connection rejected: missing auth token from ${socket.id}`);
          return next(new Error('Authentication token required'));
        }

        const decoded = jwt.verify(token, config.jwt.secret) as { userId: string; email: string };
        socket.userId = decoded.userId;

        if (deviceId && typeof deviceId === 'string') {
          socket.deviceId = deviceId;
          const device = await prisma.device.findFirst({
            where: { id: deviceId, userId: socket.userId }
          });
          if (device) {
            socket.deviceType = device.deviceType as 'CAMERA' | 'VIEWER';
          }
        }

        next();
      } catch (err: any) {
        logger.warn(`Signaling auth failure for socket ${socket.id}: ${err.message}`);
        next(new Error('Invalid authentication token'));
      }
    });

    this.io.on('connection', (socket: AuthenticatedSocket) => {
      this.handleConnection(socket);
    });

    logger.info('WebRTC Signaling Server initialized successfully on Socket.IO');
  }

  private handleConnection(socket: AuthenticatedSocket) {
    const userId = socket.userId!;
    const deviceId = socket.deviceId;

    logger.info(`Socket connected: ${socket.id} (User: ${userId}, Device: ${deviceId || 'N/A'})`);

    // Join user channel
    socket.join(`user_${userId}`);

    // If deviceId provided, join device channel and register presence
    if (deviceId) {
      this.registerDeviceConnection(deviceId, socket);
    }

    // Explicit registration event if deviceId not provided in handshake
    socket.on('register_device', async (data: { deviceId: string; deviceType: 'CAMERA' | 'VIEWER' }) => {
      try {
        const device = await prisma.device.findFirst({
          where: { id: data.deviceId, userId }
        });

        if (device) {
          socket.deviceId = device.id;
          socket.deviceType = device.deviceType as 'CAMERA' | 'VIEWER';
          this.registerDeviceConnection(device.id, socket);

          socket.emit('registered', {
            success: true,
            deviceId: device.id,
            deviceUuid: device.deviceUuid
          });
        }
      } catch (err: any) {
        logger.error(`Error registering device on socket ${socket.id}`, { error: err.message });
      }
    });

    // 1. WebRTC SDP Offer (from Camera or Viewer)
    socket.on('webrtc_offer', async (data: { targetDeviceId: string; sdp: any; sessionId?: string }) => {
      try {
        if (!socket.deviceId) {
          return socket.emit('error', { message: 'Device not registered on socket' });
        }

        logger.debug(`Relaying WebRTC Offer: from [${socket.deviceId}] to [${data.targetDeviceId}]`);

        // Check pairing authorization before forwarding offer
        const isAuthorized = await pairingService.isPairingAuthorized(
          socket.deviceType === 'CAMERA' ? socket.deviceId : data.targetDeviceId,
          socket.deviceType === 'VIEWER' ? socket.deviceId : data.targetDeviceId
        );

        if (!isAuthorized) {
          logger.warn(`Unauthorized WebRTC Offer attempt from ${socket.deviceId} to ${data.targetDeviceId}`);
          return socket.emit('webrtc_error', {
            error: 'Devices are not paired or pairing revoked'
          });
        }

        this.io?.to(`device_${data.targetDeviceId}`).emit('webrtc_offer', {
          fromDeviceId: socket.deviceId,
          fromDeviceType: socket.deviceType,
          sdp: data.sdp,
          sessionId: data.sessionId
        });
      } catch (err: any) {
        logger.error('Error handling webrtc_offer', { error: err.message });
      }
    });

    // 2. WebRTC SDP Answer (from peer)
    socket.on('webrtc_answer', (data: { targetDeviceId: string; sdp: any; sessionId?: string }) => {
      if (!socket.deviceId) return;

      logger.debug(`Relaying WebRTC Answer: from [${socket.deviceId}] to [${data.targetDeviceId}]`);

      this.io?.to(`device_${data.targetDeviceId}`).emit('webrtc_answer', {
        fromDeviceId: socket.deviceId,
        sdp: data.sdp,
        sessionId: data.sessionId
      });
    });

    // 3. ICE Candidate relay (trickle ICE)
    socket.on('ice_candidate', (data: { targetDeviceId: string; candidate: any }) => {
      if (!socket.deviceId) return;

      this.io?.to(`device_${data.targetDeviceId}`).emit('ice_candidate', {
        fromDeviceId: socket.deviceId,
        candidate: data.candidate
      });
    });

    // 4. Stream status events
    socket.on('stream_started', async (data: { targetDeviceId?: string; resolution?: string; fps?: number }) => {
      if (!socket.deviceId) return;
      logger.info(`Stream started by Camera [${socket.deviceId}]`);

      await prisma.cameraDevice.updateMany({
        where: { deviceId: socket.deviceId },
        data: {
          streamStatus: 'STREAMING',
          lastStreamStarted: new Date()
        }
      });

      await prisma.device.update({
        where: { id: socket.deviceId },
        data: { status: 'STREAMING', lastSeen: new Date() }
      });

      if (data.targetDeviceId) {
        this.io?.to(`device_${data.targetDeviceId}`).emit('stream_started', {
          cameraId: socket.deviceId,
          resolution: data.resolution || '720p',
          fps: data.fps || 30
        });
      }

      this.io?.to(`user_${userId}`).emit('device_status_changed', {
        deviceId: socket.deviceId,
        status: 'STREAMING'
      });
    });

    socket.on('stream_stopped', async (data: { targetDeviceId?: string; reason?: string }) => {
      if (!socket.deviceId) return;
      logger.info(`Stream stopped by [${socket.deviceId}], reason: ${data.reason || 'user_action'}`);

      await prisma.cameraDevice.updateMany({
        where: { deviceId: socket.deviceId },
        data: {
          streamStatus: 'IDLE',
          lastStreamEnded: new Date()
        }
      });

      await prisma.device.update({
        where: { id: socket.deviceId },
        data: { status: 'ONLINE', lastSeen: new Date() }
      });

      if (data.targetDeviceId) {
        this.io?.to(`device_${data.targetDeviceId}`).emit('stream_stopped', {
          cameraId: socket.deviceId,
          reason: data.reason || 'NORMAL'
        });
      }

      this.io?.to(`user_${userId}`).emit('device_status_changed', {
        deviceId: socket.deviceId,
        status: 'ONLINE'
      });
    });

    // 5. Camera hardware conflict / busy event (Section 18)
    socket.on('camera_busy', (data: { busy: boolean; conflictingApp?: string; targetDeviceId?: string }) => {
      if (!socket.deviceId) return;
      logger.warn(`Camera [${socket.deviceId}] reported hardware conflict: busy=${data.busy}`);

      const payload = {
        cameraId: socket.deviceId,
        busy: data.busy,
        message: data.busy
          ? 'Camera is temporarily unavailable. Another application is currently using the camera.'
          : 'Camera hardware is available again.',
        conflictingApp: data.conflictingApp
      };

      if (data.targetDeviceId) {
        this.io?.to(`device_${data.targetDeviceId}`).emit('camera_busy', payload);
      } else {
        this.io?.to(`user_${userId}`).emit('camera_busy', payload);
      }
    });

    // 6. Device heartbeat / telemetry ping
    socket.on('heartbeat', async (data: { batteryLevel?: number; networkType?: string }) => {
      if (!socket.deviceId) return;

      const now = new Date();
      await prisma.device.update({
        where: { id: socket.deviceId },
        data: { lastSeen: now }
      });

      if (socket.deviceType === 'CAMERA' && (data.batteryLevel !== undefined || data.networkType)) {
        await prisma.cameraDevice.updateMany({
          where: { deviceId: socket.deviceId },
          data: {
            ...(data.batteryLevel !== undefined ? { batteryLevel: data.batteryLevel } : {}),
            ...(data.networkType ? { networkType: data.networkType } : {})
          }
        });
      }

      socket.emit('heartbeat_ack', { timestamp: now.toISOString() });
    });

    // 7. Disconnection
    socket.on('disconnect', () => {
      logger.info(`Socket disconnected: ${socket.id} (Device: ${deviceId || 'N/A'})`);
      if (socket.deviceId) {
        this.unregisterDeviceConnection(socket.deviceId, socket.id, userId);
      }
    });
  }

  private registerDeviceConnection(deviceId: string, socket: Socket) {
    socket.join(`device_${deviceId}`);

    if (!this.connectedDevices.has(deviceId)) {
      this.connectedDevices.set(deviceId, new Set());
    }
    this.connectedDevices.get(deviceId)!.add(socket.id);

    // Update status in DB
    prisma.device
      .update({
        where: { id: deviceId },
        data: { status: 'ONLINE', lastSeen: new Date() }
      })
      .catch((err) => logger.warn(`Could not update device status for ${deviceId}: ${err.message}`));

    // Broadcast device_online
    this.io?.emit('device_online', {
      deviceId,
      timestamp: new Date().toISOString()
    });
  }

  private unregisterDeviceConnection(deviceId: string, socketId: string, userId: string) {
    const sockets = this.connectedDevices.get(deviceId);
    if (sockets) {
      sockets.delete(socketId);
      if (sockets.size === 0) {
        this.connectedDevices.delete(deviceId);

        // Mark device as OFFLINE after small grace period (10s) if no reconnect
        setTimeout(async () => {
          if (!this.connectedDevices.has(deviceId)) {
            await prisma.device
              .update({
                where: { id: deviceId },
                data: { status: 'OFFLINE', lastSeen: new Date() }
              })
              .catch(() => {});

            this.io?.to(`user_${userId}`).emit('device_offline', {
              deviceId,
              timestamp: new Date().toISOString()
            });

            logger.info(`Device marked OFFLINE: ${deviceId}`);
          }
        }, 10000);
      }
    }
  }

  notifyPairingSuccess(cameraDeviceId: string, viewerDeviceId: string, details: any) {
    this.io?.to(`device_${cameraDeviceId}`).emit('pairing_success', details);
    this.io?.to(`device_${viewerDeviceId}`).emit('pairing_success', details);
  }

  notifyPairingRevoked(cameraDeviceId: string, viewerDeviceId: string, pairingId: string) {
    this.io?.to(`device_${cameraDeviceId}`).emit('pairing_revoked', { pairingId });
    this.io?.to(`device_${viewerDeviceId}`).emit('pairing_revoked', { pairingId });
  }

  getConnectedDeviceCount(): number {
    return this.connectedDevices.size;
  }
}

export const signalingServer = new SignalingServer();
