# CoDraw Backend notes

## Cloudinary avatar upload setup

Avatar upload is implemented on the backend only.
Do **not** hardcode the Cloudinary API secret inside the Android app or commit it to source control.

### Local development with `.env`

The backend now supports loading Cloudinary settings from `CoDrawJavaBackend/.env`.

Expected keys:

- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Run locally:

```bash
cd /Users/nguyenvantoan/AndroidStudioProjects/CoDraw/CoDrawJavaBackend
./mvnw spring-boot:run
```

### Deployment / CI

For deployment, prefer real environment variables instead of a committed `.env` file.

```bash
export CLOUDINARY_CLOUD_NAME="your-cloud-name"
export CLOUDINARY_API_KEY="your-api-key"
export CLOUDINARY_API_SECRET="your-api-secret"
./mvnw spring-boot:run
```

## Profile endpoints

Authenticated endpoints added for the Android app:

- `GET /api/profile/me`
- `PUT /api/profile`
- `POST /api/profile/avatar`

Avatar uploads accept image files up to 5 MB.
