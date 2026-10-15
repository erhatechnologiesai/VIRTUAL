const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  const targetPassword = '11223344';
  const saltRounds = 10;
  const hash = await bcrypt.hash(targetPassword, saltRounds);

  const emailPairs = [
    { email: 'muhammadzeeshan0477@gmail.com', name: 'Shani' },
    { email: 'muhammadzeeshan0477@gmail.cor', name: 'Shani Cor' },
    { email: 'hja28752@gmail.com', name: 'HJA User' },
    { email: 'hja28752@gmail.cor', name: 'HJA Cor' }
  ];

  for (const item of emailPairs) {
    const user = await prisma.user.findUnique({ where: { email: item.email.toLowerCase() } });
    if (user) {
      await prisma.user.update({
        where: { id: user.id },
        data: { passwordHash: hash }
      });
      console.log(`Updated password hash for existing user ${item.email} to '11223344'`);
    } else {
      await prisma.user.create({
        data: {
          name: item.name,
          email: item.email.toLowerCase(),
          passwordHash: hash
        }
      });
      console.log(`Created user ${item.email} with password '11223344'`);
    }
  }
}

main().catch(console.error).finally(() => prisma.$disconnect());
