const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  const users = await prisma.user.findMany();
  console.log('Registered Users:');
  for (const user of users) {
    console.log(`- ID: ${user.id}, Email: "${user.email}", Name: "${user.name}"`);
  }
}

main().catch(console.error).finally(() => prisma.$disconnect());
