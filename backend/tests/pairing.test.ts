import { PairingService } from '../src/pairing/pairing.service';

describe('PairingService Unit Tests', () => {
  const pairingService = new PairingService();

  test('generatePairingCode should format as 3 letters, hyphen, 3 digits', () => {
    const code = pairingService.generatePairingCode();
    expect(code).toMatch(/^[A-Z]{3}-[0-9]{3}$/);
  });

  test('generatePairingCode should generate distinct codes', () => {
    const code1 = pairingService.generatePairingCode();
    const code2 = pairingService.generatePairingCode();
    expect(code1).not.toBe(code2);
  });
});
