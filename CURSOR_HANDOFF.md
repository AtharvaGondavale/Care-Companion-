# Care Companion — handoff for Cursor (your Mac / new machine)

Read this in a **new Cursor chat** on your laptop so the assistant knows the stack, what was built, and common commands.

**For Cursor assistants:** When the user asks how to get **secrets**, **`.env`**, **SSH keys**, or “why nothing is on GitHub”, walk them through **§ Secrets and credentials** below in order—do not assume values exist in the repo. Prefer `server/.env.example` as the checklist of variable names.

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
git clone https://github.com/AtharvaGondavale/Care-Companion-.git
cd Care-Companion-
```

(Use a different URL only if the repo was forked or renamed.)

### Android Studio

- Open the **`Care-Companion-`** folder (or the root that contains `app/`).
- **JDK 17** for Gradle (Android Studio: Settings → Build → Gradle → Gradle JDK).
- Default API base URL: **`app/build.gradle.kts`** → **`buildConfigField("String", "API_BASE_URL", "...")`** → should match production `https://api.suyashhumne.com` unless you point to another host.

### Build / run

- Run on emulator or USB device with debugging enabled.
- APK: **Build → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`

---

## Secrets and credentials — you will not get these from `git clone`

**GitHub only has code and `server/.env.example` (placeholders).** Real secrets live on machines you control (`server/.env`, SSH keys, Twilio Console). Never commit `.env`, `*.pem`, or Twilio tokens.

### What to do after cloning on your Mac

1. Create a local env file from the template (needed if you run the API via Docker Compose on your laptop, or just to edit before copying elsewhere):

```bash
cd Care-Companion-/server
cp .env.example .env
```

2. Fill **every** empty or `change-me` value using the table below. Open `.env` in an editor (`nano`, VS Code, etc.).

3. **`server/` is git-ignored for** `.env`, `node_modules/`, `dist/` — see `server/.gitignore`. If you run `git status` and see `.env` offered for commit, **stop** and fix ignore rules.

### Same file on the EC2 host (production/staging)

After the first deploy, the server directory is typically `~/care-companion-api` on Ubuntu. **Create `.env` there too** (the deploy script does **not** copy your Mac’s `.env` to the server on purpose):

```bash
ssh -i /path/to/key.pem ubuntu@<SERVER_IP>
cd ~/care-companion-api
cp .env.example .env
nano .env
```

Use **production** values (real Twilio, `TWILIO_MOCK=false` unless you intentionally mock on the server). **`DATABASE_URL`** in `server/.env.example` matches the **docker-compose** Postgres service name `postgres`; keep that shape if you use the bundled compose stack.

### Where each value comes from (guide the user)

| Variable / asset | What it is | Where the user gets it |
|------------------|------------|-------------------------|
| **`JWT_SECRET`** | Signs session tokens for the API | Generate on the Mac or server: `openssl rand -base64 32`. **Use the same value on every instance that must accept the same logins** (e.g. one secret per environment: `local` vs `prod`). If you change it in production, **everyone must log in again**. |
| **`TWILIO_ACCOUNT_SID`**, **`TWILIO_AUTH_TOKEN`**, **`TWILIO_FROM_NUMBER`** | SMS OTP | [Twilio Console](https://console.twilio.com) → Account / API keys & tokens; buy or use a trial SMS-capable number for `TWILIO_FROM_NUMBER`. |
| **`TWILIO_MOCK`** | If `true`, no real SMS; OTP is logged | Set `TWILIO_MOCK=true` in **local** `.env` for dev. Read codes with: `docker compose logs -f api` (or equivalent). Set `false` when you want real SMS. |
| Trial SMS **21608** | “Unverified number” on trial | In Twilio Console, **verify** each destination phone number, **or** use `TWILIO_MOCK=true` and read OTP from logs. |
| **`DATABASE_URL`** | Postgres connection string | **Docker Compose (default):** use the URL in `.env.example` (`postgres` hostname) when both `api` and `postgres` run in the same compose project. **Custom DB:** replace with the provider’s URL (host, user, password, DB name). |
| **`PORT`** | API listen port | Usually `3000`; Caddy reverse-proxies to this port on the host. |
| **EC2 SSH `.pem`** | Private key for `ssh` / deploy script | If **you** own AWS: EC2 → Key Pairs (you may need a new key and to attach access via console/SSM). If **someone else** provisioned the box: they must transfer the **existing** `.pem` through a **trusted channel** (password manager, encrypted archive, in person)—**not** plain email/Slack. Then: `chmod 400 /path/to/key.pem`. |

### Android app

The app does **not** embed Twilio or `JWT_SECRET`. It only needs the **public API base URL** (see `app/build.gradle.kts` → `API_BASE_URL`). No extra “secrets file” on the phone for normal login/OTP flows.

### If the user is pairing with a friend who already has production working

- **Twilio:** Friend can keep using their account, or you create your own Twilio project and put **your** SID/token/number in **your** `.env` (and redeploy).  
- **JWT / DB:** Either copy the **same** production `.env` values from a **secure** handoff (no git), or rotate secrets (new `JWT_SECRET` = everyone re-authenticates; new DB = migrate data separately).  
- **SSH:** You need **a key that EC2 accepts** (existing `.pem` or AWS-approved replacement process).

---

## Backend: local vs server

### Env file checklist

Official variable list and defaults: **`server/.env.example`**. Summary:

- **`JWT_SECRET`** — long random string (see § Secrets above).
- **Twilio** — SID, token, `TWILIO_FROM_NUMBER`; or **`TWILIO_MOCK=true`** (OTP only in **`docker compose logs api`**).
- **Trial Twilio:** verify recipient numbers or use mock mode to avoid **21608**.

Compose wiring: **`server/docker-compose.yml`** (`DATABASE_URL` must reach the compose Postgres service unless you changed topology).

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
| After `git clone`, API won’t start / no Twilio / “where are my keys?” | **Expected:** secrets are not in git. Follow **§ Secrets and credentials**: `cp server/.env.example server/.env`, fill the table, then run compose or deploy. |
| `Permission denied (publickey)` | Correct **`-i /path/key.pem`**; `chmod 400 key.pem` |
| `no configuration file` on EC2 empty dir | Run **deploy script from Mac** to populate `docker-compose.yml` |
| OTP 21608 Twilio | **Verify** recipient number on trial account |
| Gradle JDK error | Use **JDK 17** |
| Build stuck on EC2 | Add **swap** or larger instance |

---

*Generated for continuity across Cursor sessions. Update IPs, domains, and paths to match your account.*
