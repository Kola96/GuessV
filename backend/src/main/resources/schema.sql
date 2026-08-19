-- GuessV 数据库 schema（SQLite 开发环境）
-- MySQL 生产环境 DDL 在部署阶段单独维护（见 docs/architecture/004-deployment.md）

-- VTuber 团体表
CREATE TABLE IF NOT EXISTS vtuber_group (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    name_en TEXT,
    region TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- VTuber 主表
CREATE TABLE IF NOT EXISTS vtuber (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT UNIQUE NOT NULL,
    name_cn TEXT,
    name_en TEXT,
    name_jp TEXT,
    name_default TEXT,
    aliases TEXT,
    debut_year INTEGER,
    debut_date TEXT,
    region TEXT,
    group_id INTEGER,
    group_name TEXT,
    activity_status TEXT,
    gender TEXT,
    hair_color TEXT,
    eye_color TEXT,
    outfit_theme TEXT,
    fan_name TEXT,
    symbol TEXT,
    representative_color TEXT,
    platforms TEXT,
    languages TEXT,
    avatar_url TEXT,
    birthday TEXT,
    follower_count INTEGER,
    data_status TEXT,
    data_source TEXT,
    locked_fields TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_vtuber_region ON vtuber(region);
CREATE INDEX IF NOT EXISTS idx_vtuber_group_id ON vtuber(group_id);
CREATE INDEX IF NOT EXISTS idx_vtuber_data_status ON vtuber(data_status);
CREATE INDEX IF NOT EXISTS idx_vtuber_activity_status ON vtuber(activity_status);

-- 每日目标表
CREATE TABLE IF NOT EXISTS daily_target (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_date TEXT UNIQUE NOT NULL,
    vtuber_id INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户表（user 是 SQL 保留字，加双引号）
CREATE TABLE IF NOT EXISTS "user" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT UNIQUE NOT NULL,
    nickname TEXT NOT NULL,
    game_id TEXT NOT NULL,
    username TEXT UNIQUE,
    password_hash TEXT,
    email TEXT,
    oauth_provider TEXT,
    oauth_id TEXT,
    avatar_url TEXT,
    device_fingerprint TEXT,
    is_anonymous INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME,
    UNIQUE (nickname, game_id)
);

-- 游戏记录表
CREATE TABLE IF NOT EXISTS game_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    mode TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    pool_tag TEXT,
    attempts INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    is_win INTEGER NOT NULL,
    guesses TEXT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME
);
CREATE INDEX IF NOT EXISTS idx_game_record_user_id ON game_record(user_id);

-- 题库标签表
CREATE TABLE IF NOT EXISTS pool_tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tag_name TEXT UNIQUE NOT NULL,
    description TEXT,
    filter_rule TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id INTEGER,
    operation_type TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_operation_log_target ON operation_log(target_type, target_id);

-- 房间表（对战模式预留）
CREATE TABLE IF NOT EXISTS room (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_code TEXT UNIQUE NOT NULL,
    status TEXT NOT NULL,
    game_mode TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    current_players INTEGER DEFAULT 0,
    winner_id INTEGER,
    config TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    finished_at DATETIME
);

-- 房间玩家表（对战模式预留）
CREATE TABLE IF NOT EXISTS room_player (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    player_name TEXT NOT NULL,
    is_ready INTEGER DEFAULT 0,
    score INTEGER DEFAULT 0,
    finish_rank INTEGER,
    attempts_used INTEGER DEFAULT 0,
    is_winner INTEGER DEFAULT 0,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    left_at DATETIME,
    UNIQUE (room_id, user_id)
);

-- 爬虫日志表
CREATE TABLE IF NOT EXISTS crawl_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vtuber_id INTEGER,
    source TEXT,
    status TEXT,
    fields_updated TEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 管理员表
CREATE TABLE IF NOT EXISTS admin (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
