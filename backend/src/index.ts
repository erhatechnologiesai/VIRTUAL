import http from 'http';
import { createApp } from './app';
import { config } from './config';
import { logger } from './utils/logger';
import { connectDatabase, disconnectDatabase } from './database/db';
import { signalingServer } from './signaling/signaling.server';

async function bootstrap() {
  try {
    logger.info('Starting VIRTUAL Backend Server...');

    // 1. Connect to PostgreSQL via Prisma
    await connectDatabase();

    // 2. Create Express app
    const app = createApp();

    // 3. Create HTTP Server
    const httpServer = http.createServer(app);

    // 4. Initialize WebRTC Signaling on Socket.IO
    signalingServer.init(httpServer);

    // 5. Start listening
    httpServer.listen(config.port, () => {
      logger.info(`=======================================================`);
      logger.info(`  VIRTUAL Live-Camera Backend & Signaling Server       `);
      logger.info(`  Listening on port: ${config.port}                   `);
      logger.info(`  Environment: ${config.nodeEnv}                      `);
      logger.info(`  Signaling WebSocket: READY                          `);
      logger.info(`  Health check: http://localhost:${config.port}/health `);
      logger.info(`=======================================================`);

      // Keep-alive pinger to prevent tunnel activity timeout
      setInterval(() => {
        http.get(`http://127.0.0.1:${config.port}/health`, () => {}).on('error', () => {});
      }, 10000);
    });

    // Graceful Shutdown
    const shutdown = async (signal: string) => {
      logger.info(`Received ${signal}. Shutting down gracefully...`);
      httpServer.close(async () => {
        logger.info('HTTP and WebSocket servers closed');
        await disconnectDatabase();
        process.exit(0);
      });

      // Force exit after 10 seconds if hanging
      setTimeout(() => {
        logger.error('Forced exit after 10 seconds');
        process.exit(1);
      }, 10000);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  } catch (err: any) {
    logger.error('Fatal initialization error:', { error: err.message, stack: err.stack });
    process.exit(1);
  }
}

bootstrap();
