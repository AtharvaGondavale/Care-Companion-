# Care Companion — handoff for Cursor (your Mac / new machine)

Read this in a **new Cursor chat** on your laptop so the assistant knows the stack, what was built, and common commands.

---

## What this repo is

- **Android app** (`app/`): Kotlin + Jetpack Compose. Caregiver (“Guardian”) and “Elder” flows, OTP login, Retrofit to your API.
- **Backend API** (`server/`): Node 22 + **Fastify** + **Prisma** + **PostgreSQL**. OTP via **Twilio** (or `TWILIO_MOCK`), JWT auth, guardian/elder REST.

---

## URLs and infra (already set up by prior work)

- **API (HTTPS):** `https://api.suyashhumne.com`
- **EC2 Elastic IP example:** `32.236.21.83`
- **SSH user:** `ubuntu`
- **SSH key (example path on a Mac):** `~/Downloads/care-companion-ec2.pem` — use **your** actual path; **`chmod 400`** on the `.pem`.
- **Caddy:** Terminates TLS; must **`reverse_proxy 127.0.0.1:3000`** to the Docker API (not a static “placeholder” respond).
- DNS: `api` subdomain A-record → server IP.

---

## On your Mac (development)

### Clone

```bash
git clone <YOUR_GITHUB_REPO_URL> Care-Companion-
cd Care-Companion-
```

### Android Studio

- Open the **`Care-Companion-`** folder (or the root that contains `app/`).
- **JDK 17** for Gradle (Android Studio: Settings → Build → Gradle → Gradle JDK).
- Default API base URL: **`app/build.gradle.kts`** → **`buildConfigField("String", "API_BASE_URL", "...")`** → should match production `https://api.suyashhumne.com` unless you point to another host.

### Build / run

- Run on emulator or USB device with debugging enabled.
- APK: **Build → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`

---

## Backend: local vs server

### Env (server — **never commit** `.env`)

Copy on the **EC2** machine only:

```bash
cd ~/care-companion-api
cp .env.example .env
nano .env
```

Important variables (see `server/.env.example`):

- **`JWT_SECRET`** — strong random (e.g. `openssl rand -base64 32` on the server).
- **Twilio:** `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`, `TWILIO_MOCK=false`  
  OR **`TWILIO_MOCK=true`** for dev (OTP in `docker compose logs api`, no SMS).
- **Trial Twilio:** Destination numbers must be **verified** in Twilio Console or SMS fails with error **21608**.

Docker Compose sets DB URL for containers; see `server/docker-compose.yml`.

---

## Deploy API from Mac → EC2 (SSH + rsync)

From **your Mac** (paths are **local** — do **not** run this on the server):

```bash
cd /path/to/Care-Companion-/server
chmod +x scripts/deploy-via-ssh.sh

DEPLOY_HOST=32.236.21.83 \
DEPLOY_USER=ubuntu \
DEPLOY_KEY=/path/to/care-companion-ec2.pem \
./scripts/deploy-via-ssh.sh
```

- Syncs code to **`~/care-companion-api`** on EC2 **excluding** `.env` (secrets stay on server).
- Runs **`docker compose up -d --build`** on the remote host.

First time on EC2: create **`~/care-companion-api/.env`** before/after first sync.  
**Swap:** Tiny instances (t3.micro) may need **2G swap** for first `npm`/Docker build — see project chat history.

### Health checks

```bash
# On EC2
curl -sS http://127.0.0.1:3000/health

# From anywhere
curl -sS https://api.suyashhumne.com/health
```

Expect: `{"ok":true}`

### Caddy snippet

See **`server/scripts/caddy-api-reverse-proxy.snippet`**.

---

## Linking guardian ↔ elder

- Guardian creates profile with elder’s **primary phone** stored as **`linkedElderPhone`** (normalized to digits / `91` prefix for 10-digit India numbers on the server).
- **Add profile** screen: **Get OTP → Verify OTP** (real API, role `ELDER`) before save when guardian is logged in — does **not** replace guardian JWT.
- Elder logs in with the **same** phone number → `/v1/elder/me`, medicines, contacts.

---

## Repo layout (quick)

| Path | Role |
|------|------|
| `app/` | Android application |
| `server/src/index.ts` | Fastify routes |
| `server/prisma/` | Schema + migrations |
| `server/docker-compose.yml` | Postgres + API |
| `server/scripts/deploy-via-ssh.sh` | Mac → EC2 deploy |
| `server/.env.example` | Template for server env |

---

## Security reminders

- **Never commit** `.env`, Twilio secrets, or **`.pem`** keys.
- If tokens were pasted in screenshots, **rotate** Twilio Auth Token and `JWT_SECRET` if leaked.

---

## Common issues

| Symptom | Likely fix |
|---------|-------------|
| `Permission denied (publickey)` | Correct **`-i /path/key.pem`**; `chmod 400 key.pem` |
| `no configuration file` on EC2 empty dir | Run **deploy script from Mac** to populate `docker-compose.yml` |
| OTP 21608 Twilio | **Verify** recipient number on trial account |
| Gradle JDK error | Use **JDK 17** |
| Build stuck on EC2 | Add **swap** or larger instance |

---

*Generated for continuity across Cursor sessions. Update IPs, domains, and paths to match your account.*
