# Source-free deployment: insurance.gkbuilds.in

The VPS runs prebuilt images from **gk1106/insurance-ai-assistant**. It needs neither the Git repository nor Java/Node build tools. The existing project at api.gkbuilds.in remains a separate Compose stack and Nginx vhost.

## What GitHub builds

The existing `.github/workflows/ci.yml` now tests the backend, validates/builds the frontend, and publishes these images on successful main pushes (or a manual main workflow run):

- `ghcr.io/gk1106/insurance-ai-assistant-backend:<commit-sha>`
- `ghcr.io/gk1106/insurance-ai-assistant-frontend:<commit-sha>`

PR images are built but never published or deployed. Both images use the same full Git SHA. The frontend image receives https://insurance.gkbuilds.in at build time, so sharing metadata uses the correct origin. If the domain changes, update PUBLIC_SITE_URL in the workflow and publish new images; changing SITE_URL only in the VPS .env cannot rewrite an already-built frontend. Database credentials, JWT secret and OpenAI key remain on the VPS and are never image build arguments.

After both images publish, the workflow updates convenience `latest` tags and creates an **insurance-server-COMMIT_SHA** artifact. Use the artifact's SHA for a consistent release; don't deploy while publishing is still in progress. Builds target Linux AMD64 (check `uname -m` on the VPS; x86_64 is supported).

## One-time server files

Download the successful workflow's server artifact. Copy its contents into `/opt/insurance`, owned by the same account that will run Docker/SSH deployment. Separately copy the locally generated root `.env` into that folder; Git and the artifact deliberately exclude it.

```text
/opt/insurance/
  docker-compose.prod.yml
  update.sh
  release-tag.txt
  .env                       # your generated production values
  documents/                 # optional insurance source PDFs, persistent
  nginx/insurance.gkbuilds.in.conf
```

Fill `OPENAI_API_KEY` in `.env`. Review `APP_BOOTSTRAP_ADMIN_EMAIL`. The admin username is `insurance_admin`, with the generated password in `.env`. Existing backend/.env is for development and is not required on the VPS. Keep the SAME production `.env` on future updates.

The runtime Compose file contains **image references only**, with no build instructions or application source mounts. Browser traffic goes through host Nginx to 127.0.0.1:8085; Spring Boot (8082) and PostgreSQL with pgvector (5432) stay inside Docker. The `insurance` project name preserves this stack's separate network and PostgreSQL volume.

## One-time GHCR access

New GHCR packages can be private. While signed in as the SAME VPS user used for deployments, log in with GitHub username `gk1106` and a personal access token (classic) with `read:packages` access to these packages. If the packages are public, registry login is unnecessary.

```sh
read -rsp 'GHCR read token: ' GHCR_READ_TOKEN; echo
printf '%s' "$GHCR_READ_TOKEN" | docker login ghcr.io -u gk1106 --password-stdin
unset GHCR_READ_TOKEN
```

The image-publishing workflow itself uses GitHub's built-in `GITHUB_TOKEN` with packages:write; you do not need to add an extra registry token to GitHub Actions. If a package already exists and publishing is denied, grant this repository Actions access to that package in its package settings.

## First start and manual updates

Docker Compose v2, Bash and `flock` (util-linux on AlmaLinux) must be installed. From `/opt/insurance`:

```sh
chmod 600 .env
chmod 700 update.sh
bash update.sh
```

With no argument the helper uses the SHA in `release-tag.txt`. For a later published release:

```sh
bash update.sh FULL_40_CHARACTER_COMMIT_SHA
```

The helper pulls BOTH application images successfully before recreating any containers, waits for health/readiness, and leaves the database volume intact. It never runs `down -v` or a global prune. A deployment that fails health checks stops with an error for inspection; it does not attempt an unsafe automatic database rollback. Docker downloads changed layers rather than transferring the source folder.

Useful checks:

```sh
cat .last-deployed-tag
docker compose --env-file .env -p insurance -f docker-compose.prod.yml ps
curl -I http://127.0.0.1:8085/
```

`IMAGE_TAG` is exported only by the update helper. For raw Compose commands needing image references, export `IMAGE_TAG` from `.last-deployed-tag` first; don't accidentally switch to the default `latest` release. Roll back app images by passing a previous published SHA to `update.sh`. Image rollback does not reverse Flyway database migrations; check schema compatibility and back up PostgreSQL before schema-changing releases.

The database/admin passwords initialize NEW records. Editing `.env` later does not rotate existing database or account passwords. Production bootstrap creates the configured admin if absent and disables only the untouched V2 demo login. Readiness stays unavailable until bootstrap finishes. Existing V3/V5 demo customer data remains present. Don't delete the database volume on updates.

If you already used the old source-building deployment, keep the SAME `-p insurance` project and production .env to retain `insurance_postgres_data`. Move any existing source PDFs from `backend/documents` into the new `documents` directory once. New bind mounts are read-only to Java and use SELinux `Z` labeling on AlmaLinux. This is source-free runtime data, not a backend project folder.

## Optional: automatic deployment after every main push

Complete the one-time server setup and registry login first. Add repository secrets at **GitHub -> Settings -> Secrets and variables -> Actions -> Secrets**:

| Secret | Value |
| --- | --- |
| `VPS_HOST` | VPS IP or SSH hostname |
| `VPS_USER` | The VPS account owning /opt/insurance and permitted to run Docker |
| `VPS_SSH_KEY` | Private key for a dedicated deployment SSH key; install its public key on that VPS account |
| `VPS_KNOWN_HOSTS` | Verified SSH known_hosts line(s), obtained from the VPS's host key/fingerprint over a trusted channel; use `[host]:port` format for non-default SSH ports |

Add repository **Variables**:

| Variable | Value |
| --- | --- |
| `VPS_DEPLOY_ENABLED` | `true` to opt in; absent/false leaves automatic SSH deployment off |
| `VPS_PORT` | `22` unless SSH uses another port |

The workflow uses the `production` GitHub environment, so environment-level approval rules can be configured if desired. Once enabled, a successful main push copies only the small Compose/update files over SSH, then pulls and starts the exact published SHA. It never uploads source code, .env, database data, documents, or the live Nginx config. It uses strict host-key checking rather than blindly trusting a newly scanned key.

## Host Nginx and SSL

This template assumes Nginx runs directly on the AlmaLinux VPS host. If Nginx itself runs in Docker, use a shared Docker proxy network and frontend service name instead; localhost inside that proxy container does not refer to the host frontend port.

Point insurance.gkbuilds.in to the VPS. Copy `nginx/insurance.gkbuilds.in.conf` (from the artifact) or `deploy/nginx/insurance.gkbuilds.in.conf` (from this repository) into `/etc/nginx/conf.d/`. Ensure a certificate covers this subdomain, and adapt the certificate paths to the existing SSL configuration. Keep the existing api.gkbuilds.in vhost.

```sh
sudo nginx -t
sudo systemctl reload nginx
```

If SELinux enforcing blocks host Nginx's upstream connection, permit Nginx network connections without disabling SELinux:

```sh
sudo setsebool -P httpd_can_network_connect 1
```

Only HTTP/HTTPS need public firewall ports; don't expose 8085, 8082 or PostgreSQL. Both proxy layers allow 180 seconds for AI calls. DNS, SSL, RAM, registry access and actual runtime health still need verification on the VPS. No images or deployment were published from this local configuration task.

References: [GitHub image publishing](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images), [GHCR authentication](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry), [Compose up](https://docs.docker.com/reference/cli/docker/compose/up/).
