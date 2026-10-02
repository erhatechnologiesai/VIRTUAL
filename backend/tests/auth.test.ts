import jwt from 'jsonwebtoken';
import { AuthService } from '../src/auth/auth.service';
import { config } from '../src/config';

describe('AuthService Unit Tests', () => {
  const authService = new AuthService();

  test('generateTokens should return valid access and refresh tokens', () => {
    const userId = 'user-12345';
    const email = 'test@virtualcam.io';

    const tokens = authService.generateTokens(userId, email);

    expect(tokens.accessToken).toBeDefined();
    expect(tokens.refreshToken).toBeDefined();
    expect(tokens.tokenType).toBe('Bearer');

    // Verify access token
    const decodedAccess = jwt.verify(tokens.accessToken, config.jwt.secret) as any;
    expect(decodedAccess.userId).toBe(userId);
    expect(decodedAccess.email).toBe(email);

    // Verify refresh token
    const decodedRefresh = jwt.verify(tokens.refreshToken, config.jwt.refreshSecret) as any;
    expect(decodedRefresh.userId).toBe(userId);
    expect(decodedRefresh.email).toBe(email);
  });
});
