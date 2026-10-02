import winston from 'winston';
import { config } from '../config';

const { combine, timestamp, printf, colorize, errors } = winston.format;

// Custom log format for readable and structured logging
const customFormat = printf(({ level, message, timestamp, stack, ...meta }) => {
  const metaStr = Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : '';
  const errorStack = stack ? `\n${stack}` : '';
  return `[${timestamp}] [${level}]: ${message}${metaStr}${errorStack}`;
});

export const logger = winston.createLogger({
  level: config.logLevel,
  format: combine(
    timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    errors({ stack: true }),
    customFormat
  ),
  defaultMeta: { service: 'virtual-backend' },
  transports: [
    new winston.transports.Console({
      format: combine(
        colorize(),
        timestamp({ format: 'HH:mm:ss' }),
        customFormat
      )
    })
  ]
});
