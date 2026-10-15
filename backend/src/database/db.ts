import { PrismaClient } from '@prisma/client';
import { logger } from '../utils/logger';

declare global {
  // eslint-disable-next-line no-var
  var prismaInstance: PrismaClient | undefined;
}

export const prisma =
  global.prismaInstance ||
  new PrismaClient({
    log: [
      { emit: 'event', level: 'query' },
      { emit: 'event', level: 'error' },
      { emit: 'event', level: 'info' },
      { emit: 'event', level: 'warn' }
    ]
  });

if (process.env.NODE_ENV !== 'production') {
  global.prismaInstance = prisma;
}

// Log queries and handle idle connection drops gracefully
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(prisma as any).$on('error', async (e: any) => {
  if (!e.message) return;
  if (e.message.includes('Closed') || e.message.includes('connection')) {
    // Neon Cloud PostgreSQL idle pool reset - reconnect quietly in background
    try {
      await prisma.$connect();
    } catch {}
    return;
  }
  if (e.message.includes('Record to update not found') || e.message.includes('Record to delete does not exist')) {
    return;
  }
  logger.error('Prisma Error', { error: e.message });
});

let keepAliveInterval: NodeJS.Timeout | null = null;

export async function connectDatabase(): Promise<void> {
  try {
    await prisma.$connect();
    logger.info('Database connected successfully via Prisma');

    // Keep-alive heartbeat every 45 seconds to keep Neon Serverless Pool connection warm
    if (!keepAliveInterval) {
      keepAliveInterval = setInterval(async () => {
        try {
          await prisma.$queryRaw`SELECT 1`;
        } catch {
          try {
            await prisma.$connect();
          } catch {}
        }
      }, 45000);
    }
  } catch (err: any) {
    logger.warn('Database connection warning (will retry upon request): ' + err.message);
  }
}

export async function disconnectDatabase(): Promise<void> {
  if (keepAliveInterval) {
    clearInterval(keepAliveInterval);
    keepAliveInterval = null;
  }
  await prisma.$disconnect();
  logger.info('Database disconnected');
}
