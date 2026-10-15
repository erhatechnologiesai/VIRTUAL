FROM node:20-alpine

WORKDIR /app

# Copy backend dependencies manifests
COPY backend/package*.json ./
COPY backend/prisma ./prisma/

# Install dependencies & generate Prisma client
RUN npm install
RUN npx prisma generate

# Copy source files and compile TypeScript
COPY backend/tsconfig.json ./
COPY backend/src ./src/
RUN npm run build

EXPOSE 8000 4000 10000

ENV PORT=8000
ENV NODE_ENV=production

CMD ["node", "dist/index.js"]
