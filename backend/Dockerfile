FROM node:20-alpine

WORKDIR /app

# Copy package manifests & prisma schema
COPY package*.json ./
COPY prisma ./prisma/

# Install dependencies & generate Prisma client
RUN npm install
RUN npx prisma generate

# Copy source and compile TypeScript
COPY tsconfig.json ./
COPY src ./src/
RUN npm run build

EXPOSE 8000 4000

ENV PORT=8000
ENV NODE_ENV=production

CMD ["node", "dist/index.js"]
