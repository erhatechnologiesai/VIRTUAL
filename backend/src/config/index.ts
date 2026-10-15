import dotenv from 'dotenv';
import path from 'path';

// Load .env
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

export const config = {
  port: parseInt(process.env.PORT || '4000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  isProduction: process.env.NODE_ENV === 'production',
  logLevel: process.env.LOG_LEVEL || 'info',

  databaseUrl: process.env.DATABASE_URL || 'postgresql://virtual_user:virtual_secure_password@localhost:5432/virtual_db?schema=public',

  jwt: {
    secret: process.env.JWT_SECRET || 'dev_jwt_secret_virtual_app_key_2026_test_only',
    refreshSecret: process.env.REFRESH_TOKEN_SECRET || 'dev_refresh_token_secret_virtual_app_key_2026_test_only',
    expiresIn: process.env.JWT_EXPIRES_IN || '1h',
    refreshExpiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '30d'
  },

  webrtc: {
    stunUrl: process.env.STUN_SERVER_URL || 'stun:stun.l.google.com:19302',
    turnUrl: process.env.TURN_SERVER_URL || '',
    turnUsername: process.env.TURN_USERNAME || '',
    turnPassword: process.env.TURN_PASSWORD || ''
  },

  firebase: {
    projectId: process.env.FIREBASE_PROJECT_ID || '',
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL || '',
    privateKey: process.env.FIREBASE_PRIVATE_KEY || ''
  },

  corsOrigin: process.env.CORS_ORIGIN || '*'
};
