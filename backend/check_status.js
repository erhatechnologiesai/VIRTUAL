const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function check() {
  console.log('=== USERS ===');
  const users = await prisma.user.findMany();
  console.log(users.map(u => ({ id: u.id, email: u.email, name: u.name })));

  console.log('\n=== DEVICES ===');
  const devices = await prisma.device.findMany({ include: { cameraDetail: true } });
  console.log(devices.map(d => ({
    id: d.id,
    userId: d.userId,
    deviceUuid: d.deviceUuid,
    deviceName: d.deviceName,
    deviceType: d.deviceType,
    status: d.status,
    lastSeen: d.lastSeen
  })));

  console.log('\n=== PAIRINGS ===');
  const pairings = await prisma.pairing.findMany();
  console.log(pairings);

  console.log('\n=== SESSIONS ===');
  const sessions = await prisma.session.findMany({ take: 10, orderBy: { startedAt: 'desc' } });
  console.log(sessions);
}

check().catch(console.error).finally(() => prisma.$disconnect());
