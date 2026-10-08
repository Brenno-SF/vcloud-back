CREATE TABLE users (
   id UUID PRIMARY KEY,
   username VARCHAR(100) NOT NULL UNIQUE,
   email VARCHAR(255) NOT NULL UNIQUE,
   password_hash VARCHAR(255) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE videos (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,

    original_filename VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL UNIQUE,

    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_video_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_videos_user_id ON videos(user_id);

CREATE INDEX idx_videos_created_at ON videos(created_at DESC);

CREATE INDEX idx_videos_user_created ON videos(user_id, created_at DESC);