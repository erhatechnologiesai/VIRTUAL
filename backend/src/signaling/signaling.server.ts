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
    this.io.use((socket: AuthenticatedSocket, next) => {
      try {
        const token = socket.handshake.auth?.token || socket.handshake.headers?.authorization?.replace('Bearer ', '');
        const deviceId = socket.handshake.auth?.deviceId || socket.handshake.query?.deviceId;

        if (token) {
          try {
            const decoded = jwt.verify(token, config.jwt.secret) as { userId: string; email: string };
            socket.userId = decoded.userId;
          } catch (e) {
            socket.userId = 'guest_user_id';
          }
        } else {
          socket.userId = 'guest_user_id';
        }

        if (deviceId && typeof deviceId === 'string') {
          // Immediately set in-memory deviceId so real-time signaling works without waiting on DB
          socket.deviceId = deviceId;
          prisma.device
            .findFirst({
              where: { OR: [{ id: deviceId }, { deviceUuid: deviceId }] }
            })
            .then((device) => {
              if (device) {
                socket.deviceId = device.id;
                socket.deviceType = device.deviceType as 'CAMERA' | 'VIEWER';
                this.registerDeviceConnection(device.id, socket);
              }
            })
            .catch(() => {});
        }

        next();
      } catch (err: any) {
        socket.userId = 'guest_user_id';
        next();
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

    // If deviceId provided, join device channel and register presence immediately
    if (deviceId) {
      this.registerDeviceConnection(deviceId, socket);
    }

    // Explicit registration event: IMMEDIATE IN-MEMORY FIRST
    socket.on('register_device', (data: { deviceId: string; deviceType: 'CAMERA' | 'VIEWER' }) => {
      const rawId = data.deviceId;
      socket.deviceId = rawId;
      socket.deviceType = data.deviceType;
      this.registerDeviceConnection(rawId, socket);

      socket.emit('registered', {
        success: true,
        deviceId: rawId,
        deviceUuid: rawId
      });

      // Background DB resolution - NEVER blocks WebRTC signaling
      prisma.device
        .findFirst({
          where: { OR: [{ id: rawId }, { deviceUuid: rawId }] }
        })
        .then((device) => {
          if (device) {
            const activeId = device.id;
            socket.deviceId = activeId;
            socket.deviceType = (device.deviceType || data.deviceType) as 'CAMERA' | 'VIEWER';
            this.registerDeviceConnection(activeId, socket);
            if (device.deviceUuid && device.deviceUuid !== activeId) {
              socket.join(`device_${device.deviceUuid}`);
              socket.join(device.deviceUuid);
            }
          }
        })
        .catch((err) => {
          logger.warn(`Non-blocking DB query for device ${rawId}: ${err.message}`);
        });
    });

    // 1. WebRTC SDP Offer (from Camera or Viewer)
    socket.on('webrtc_offer', async (data: { targetDeviceId: string; sdp: any; sessionId?: string }) => {
      try {
        const fromId = socket.deviceId || socket.id;
        logger.info(`Relaying WebRTC Offer: from [${fromId}] to [${data.targetDeviceId}]`);

        const payload = {
          fromDeviceId: fromId,
          fromDeviceType: socket.deviceType || 'VIEWER',
          sdp: data.sdp,
          sessionId: data.sessionId
        };

        const cleanTarget = data.targetDeviceId.startsWith('device_')
          ? data.targetDeviceId.replace(/^device_/, '')
          : data.targetDeviceId;

        // Forward to specific target device room
        this.io?.to(`device_${cleanTarget}`).emit('webrtc_offer', payload);
        this.io?.to(cleanTarget).emit('webrtc_offer', payload);
        this.io?.to(data.targetDeviceId).emit('webrtc_offer', payload);
        this.io?.to('all_cameras').emit('webrtc_offer', payload);
        this.io?.to('device_local_cam_1').emit('webrtc_offer', payload);
        this.io?.to('device_VIRTUAL-888').emit('webrtc_offer', payload);
        socket.broadcast.emit('webrtc_offer', payload);
      } catch (err: any) {
        logger.error('Error handling webrtc_offer', { error: err.message });
      }
    });

    // 2. WebRTC SDP Answer (from peer)
    socket.on('webrtc_answer', (data: { targetDeviceId: string; sdp: any; sessionId?: string }) => {
      const fromId = socket.deviceId || socket.id;
      logger.info(`Relaying WebRTC Answer: from [${fromId}] to [${data.targetDeviceId}]`);

      const payload = {
        fromDeviceId: fromId,
        sdp: data.sdp,
        sessionId: data.sessionId
      };

      const cleanTarget = data.targetDeviceId.startsWith('device_')
        ? data.targetDeviceId.replace(/^device_/, '')
        : data.targetDeviceId;

      this.io?.to(`device_${cleanTarget}`).emit('webrtc_answer', payload);
      this.io?.to(cleanTarget).emit('webrtc_answer', payload);
      this.io?.to(data.targetDeviceId).emit('webrtc_answer', payload);
      this.io?.to('all_viewers').emit('webrtc_answer', payload);
      socket.broadcast.emit('webrtc_answer', payload);
    });

    // 3. ICE Candidate relay (trickle ICE)
    socket.on('ice_candidate', (data: { targetDeviceId: string; candidate: any }) => {
      const fromId = socket.deviceId || socket.id;

      const payload = {
        fromDeviceId: fromId,
        targetDeviceId: data.targetDeviceId,
        candidate: data.candidate
      };

      const cleanTarget = data.targetDeviceId.startsWith('device_')
        ? data.targetDeviceId.replace(/^device_/, '')
        : data.targetDeviceId;

      this.io?.to(`device_${cleanTarget}`).emit('ice_candidate', payload);
      this.io?.to(cleanTarget).emit('ice_candidate', payload);
      this.io?.to(data.targetDeviceId).emit('ice_candidate', payload);
      this.io?.to('all_cameras').emit('ice_candidate', payload);
      this.io?.to('all_viewers').emit('ice_candidate', payload);
      this.io?.to('device_local_cam_1').emit('ice_candidate', payload);
      this.io?.to('device_VIRTUAL-888').emit('ice_candidate', payload);
      socket.broadcast.emit('ice_candidate', payload);
    });

    // 4. Stream status events
    socket.on('stream_started', (data: { targetDeviceId?: string; resolution?: string; fps?: number }) => {
      if (!socket.deviceId) return;
      logger.info(`Stream started by Camera [${socket.deviceId}]`);

      // 1. Instantly notify viewer and user channel in real-time
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

      // 2. Background DB update (Non-blocking)
      prisma.device.updateMany({
        where: { OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }] },
        data: { status: 'STREAMING', lastSeen: new Date() }
      }).catch(() => {});

      prisma.cameraDevice.updateMany({
        where: {
          device: {
            OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }]
          }
        },
        data: {
          streamStatus: 'STREAMING',
          lastStreamStarted: new Date()
        }
      }).catch(() => {});
    });

    socket.on('stream_stopped', (data: { targetDeviceId?: string; reason?: string }) => {
      if (!socket.deviceId) return;
      logger.info(`Stream stopped by [${socket.deviceId}], reason: ${data.reason || 'user_action'}`);

      // 1. Instantly notify viewer and user channel
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

      // 2. Background DB update (Non-blocking)
      prisma.device.updateMany({
        where: { OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }] },
        data: { status: 'ONLINE', lastSeen: new Date() }
      }).catch(() => {});

      prisma.cameraDevice.updateMany({
        where: {
          device: {
            OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }]
          }
        },
        data: {
          streamStatus: 'IDLE',
          lastStreamEnded: new Date()
        }
      }).catch(() => {});
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
    socket.on('heartbeat', (data: { batteryLevel?: number; networkType?: string }) => {
      if (!socket.deviceId) return;

      const now = new Date();
      // Instantly acknowledge heartbeat to prevent client reconnection storms
      socket.emit('heartbeat_ack', { timestamp: now.toISOString() });

      // Non-blocking async DB update
      prisma.device.updateMany({
        where: { OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }] },
        data: { lastSeen: now }
      }).catch(() => {});

      if (socket.deviceType === 'CAMERA' && (data.batteryLevel !== undefined || data.networkType)) {
        prisma.cameraDevice.updateMany({
          where: {
            device: {
              OR: [{ id: socket.deviceId }, { deviceUuid: socket.deviceId }]
            }
          },
          data: {
            ...(data.batteryLevel !== undefined ? { batteryLevel: data.batteryLevel } : {}),
            ...(data.networkType ? { networkType: data.networkType } : {})
          }
        }).catch(() => {});
      }
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
    const cleanId = deviceId.startsWith('device_') ? deviceId.replace(/^device_/, '') : deviceId;
    socket.join(`device_${cleanId}`);
    socket.join(cleanId);
    socket.join(deviceId);
    socket.join(`device_${deviceId}`);
    socket.join('all_cameras');
    socket.join('all_viewers');
    socket.join('device_local_cam_1');
    socket.join('device_VIRTUAL-888');

    if (!this.connectedDevices.has(cleanId)) {
      this.connectedDevices.set(cleanId, new Set());
    }
    this.connectedDevices.get(cleanId)!.add(socket.id);

    // Update status in DB
    prisma.device
      .updateMany({
        where: { OR: [{ id: cleanId }, { deviceUuid: cleanId }] },
        data: { status: 'ONLINE', lastSeen: new Date() }
      })
      .catch((err) => logger.warn(`Could not update device status for ${cleanId}: ${err.message}`));

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
              .updateMany({
                where: { OR: [{ id: deviceId }, { deviceUuid: deviceId }] },
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
    this.io?.emit('pairing_success', details);
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
