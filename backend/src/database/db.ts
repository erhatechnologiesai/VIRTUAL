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

// Log queries in development
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(prisma as any).$on('error', (e: any) => {
  logger.error('Prisma Error', { error: e.message });
});

export async function connectDatabase(): Promise<void> {
  try {
    await prisma.$connect();
    logger.info('Database connected successfully via Prisma');
  } catch (err: any) {
    logger.warn('Database connection warning (will retry upon request): ' + err.message);
  }
}

export async function disconnectDatabase(): Promise<void> {
  await prisma.$disconnect();
  logger.info('Database disconnected');
}
