#!/usr/bin/env sh
set -eu
npm exec prisma migrate deploy
exec node dist/index.js
