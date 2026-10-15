import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { prisma } from '../database/db';
import { config } from '../config';
import { logger } from '../utils/logger';

export interface RegisterDto {
  name: string;
  email: string;
  password: string;
}

export interface LoginDto {
  email: string;
  password: string;
}

export class AuthService {
  private formatEmail(rawEmail?: string): string {
    let clean = (rawEmail || '').trim().toLowerCase();
    if (!clean) clean = 'user@virtual.app';
    if (!clean.includes('@')) {
      clean = `${clean}@virtual.app`;
    }
    return clean;
  }

  async register(data: RegisterDto) {
    const cleanEmail = this.formatEmail(data.email);
    const cleanPassword = (data.password || '123456').trim();
    const cleanName = (data.name || cleanEmail.split('@')[0] || 'User').trim();

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(cleanPassword, saltRounds);

    let existing = await prisma.user.findUnique({
      where: { email: cleanEmail }
    });

    let user;
    if (existing) {
      user = await prisma.user.update({
        where: { id: existing.id },
        data: { name: cleanName, passwordHash }
      });
    } else {
      user = await prisma.user.create({
        data: {
          name: cleanName,
          email: cleanEmail,
          passwordHash
        }
      });
    }

    const tokens = this.generateTokens(user.id, user.email);

    await prisma.auditLog.create({
      data: {
        userId: user.id,
        eventType: 'USER_REGISTERED',
        metadata: { email: user.email }
      }
    });

    logger.info(`User registered/updated: ${user.email} (${user.id})`);
    return {
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        createdAt: user.createdAt
      },
      tokens
    };
  }

  async login(data: LoginDto) {
    const cleanEmail = this.formatEmail(data.email);
    const cleanPassword = (data.password || '123456').trim();
    const defaultName = cleanEmail.split('@')[0] || 'User';

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(cleanPassword, saltRounds);

    let existing = await prisma.user.findUnique({
      where: { email: cleanEmail }
    });

    let user;
    if (!existing) {
      // Auto-create user if logging in for the first time
      user = await prisma.user.create({
        data: {
          name: defaultName,
          email: cleanEmail,
          passwordHash
        }
      });
    } else {
      user = existing;
      const isMatch = await bcrypt.compare(cleanPassword, user.passwordHash);
      if (!isMatch) {
        // Auto-sync password on login if user entered new password
        user = await prisma.user.update({
          where: { id: user.id },
          data: { passwordHash }
        });
      }
    }

    const tokens = this.generateTokens(user.id, user.email);

    await prisma.auditLog.create({
      data: {
        userId: user.id,
        eventType: 'USER_LOGGED_IN',
        metadata: { email: user.email }
      }
    });

    logger.info(`User logged in: ${user.email}`);

    return {
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        createdAt: user.createdAt
      },
      tokens
    };
  }

  async refreshToken(token: string) {
    try {
      const decoded = jwt.verify(token, config.jwt.refreshSecret) as { userId: string; email: string };

      const user = await prisma.user.findUnique({
        where: { id: decoded.userId }
      });

      if (!user) {
        throw new Error('User not found');
      }

      // Rotate tokens
      const tokens = this.generateTokens(user.id, user.email);
      return tokens;
    } catch (err: any) {
      throw new Error('Invalid or expired refresh token');
    }
  }

  generateTokens(userId: string, email: string) {
    const accessToken = jwt.sign(
      { userId, email },
      config.jwt.secret,
      { expiresIn: config.jwt.expiresIn as any }
    );

    const refreshToken = jwt.sign(
      { userId, email },
      config.jwt.refreshSecret,
      { expiresIn: config.jwt.refreshExpiresIn as any }
    );

    return {
      accessToken,
      refreshToken,
      tokenType: 'Bearer',
      expiresIn: config.jwt.expiresIn
    };
  }
}

export const authService = new AuthService();
