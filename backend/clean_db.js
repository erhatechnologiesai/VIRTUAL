const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  console.log('Cleaning up all database tables...');
  await prisma.auditLog.deleteMany({});
  await prisma.session.deleteMany({});
  await prisma.pairing.deleteMany({});
  await prisma.cameraDevice.deleteMany({});
  await prisma.pushToken.deleteMany({});
  await prisma.deviceSetting.deleteMany({});
  await prisma.device.deleteMany({});
  await prisma.user.deleteMany({});
  console.log('Database successfully cleaned! All records deleted.');
}

main().catch(console.error).finally(() => prisma.$disconnect());
