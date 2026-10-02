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
  async register(data: RegisterDto) {
    const existing = await prisma.user.findUnique({
      where: { email: data.email.toLowerCase() }
    });

    if (existing) {
      throw new Error('Email is already registered');
    }

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(data.password, saltRounds);

    const user = await prisma.user.create({
      data: {
        name: data.name,
        email: data.email.toLowerCase(),
        passwordHash
      },
      select: {
        id: true,
        name: true,
        email: true,
        createdAt: true
      }
    });

    // Generate tokens
    const tokens = this.generateTokens(user.id, user.email);

    // Record audit log
    await prisma.auditLog.create({
      data: {
        userId: user.id,
        eventType: 'USER_REGISTERED',
        metadata: { email: user.email }
      }
    });

    logger.info(`User registered: ${user.email} (${user.id})`);

    return { user, tokens };
  }

  async login(data: LoginDto) {
    const user = await prisma.user.findUnique({
      where: { email: data.email.toLowerCase() }
    });

    if (!user) {
      throw new Error('Invalid email or password');
    }

    const isMatch = await bcrypt.compare(data.password, user.passwordHash);
    if (!isMatch) {
      throw new Error('Invalid email or password');
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
