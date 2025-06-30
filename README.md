# 🌊 Surf

**Surf** is a lightweight, self-hosted Maven-compatible build service.

It automatically clones, builds, and serves **Java/Kotlin libraries from GitHub** - making them accessible as dependencies in any Maven or Gradle project.

Runs fully in Docker, no manual setup required.

---

## 🚀 Features

- ✅ Build Kotlin/Java libraries from public GitHub repos
- ✅ Serve them via HTTP as a Maven-compatible repository
- ✅ Runs 100% inside Docker
- ✅ No database, no config files, no login

---

## 🧰 Usage with Docker Compose

### 1. Create `.env`

```env
SURF_REPO_1=https://github.com/your-org/your-kotlin-lib
SURF_REPO_2=https://github.com/another-dev/java-lib