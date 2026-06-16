CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS nest_cart (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    boom_length DOUBLE PRECISION NOT NULL DEFAULT 8.0,
    boom_cross_section_area DOUBLE PRECISION NOT NULL DEFAULT 0.01,
    boom_moment_of_inertia DOUBLE PRECISION NOT NULL DEFAULT 8.33e-6,
    boom_elastic_modulus DOUBLE PRECISION NOT NULL DEFAULT 1.2e10,
    basket_weight DOUBLE PRECISION NOT NULL DEFAULT 150.0,
    base_height DOUBLE PRECISION NOT NULL DEFAULT 4.0,
    max_height DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    stress_limit DOUBLE PRECISION NOT NULL DEFAULT 8.0e6,
    sway_limit DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    crew_capacity INTEGER DEFAULT 3,
    dynasty VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE nest_cart ADD COLUMN IF NOT EXISTS crew_capacity INTEGER DEFAULT 3;
ALTER TABLE nest_cart ADD COLUMN IF NOT EXISTS dynasty VARCHAR(50);

CREATE TABLE IF NOT EXISTS dynasty_cart (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dynasty_name VARCHAR(50) NOT NULL,
    period VARCHAR(100) NOT NULL,
    era_year VARCHAR(100),
    historical_context TEXT,
    boom_length DOUBLE PRECISION,
    boom_cross_section_area DOUBLE PRECISION,
    boom_moment_of_inertia DOUBLE PRECISION,
    boom_elastic_modulus DOUBLE PRECISION,
    basket_weight DOUBLE PRECISION,
    base_height DOUBLE PRECISION,
    max_height DOUBLE PRECISION,
    stress_limit DOUBLE PRECISION,
    sway_limit DOUBLE PRECISION,
    crew_size INTEGER,
    observation_distance_estimate DOUBLE PRECISION,
    construction_material VARCHAR(100),
    military_role VARCHAR(200),
    historical_record TEXT,
    innovation_features TEXT,
    evolution_score DOUBLE PRECISION,
    sort_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS modern_drone_spec (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100),
    category VARCHAR(50),
    year_introduced INTEGER,
    max_flight_altitude_m DOUBLE PRECISION,
    max_ceiling_m DOUBLE PRECISION,
    max_flight_range_km DOUBLE PRECISION,
    flight_endurance_minutes DOUBLE PRECISION,
    cruise_speed_kmh DOUBLE PRECISION,
    max_speed_kmh DOUBLE PRECISION,
    camera_resolution_mp DOUBLE PRECISION,
    optical_zoom DOUBLE PRECISION,
    thermal_camera BOOLEAN DEFAULT FALSE,
    thermal_resolution_mp DOUBLE PRECISION,
    thermal_sensitivity_mk DOUBLE PRECISION,
    surveillance_radius_km DOUBLE PRECISION,
    data_link_range_km DOUBLE PRECISION,
    payload_capacity_kg DOUBLE PRECISION,
    takeoff_weight_kg DOUBLE PRECISION,
    unit_cost_usd DOUBLE PRECISION,
    operating_cost_per_hour_usd DOUBLE PRECISION,
    crew_required INTEGER,
    setup_time_minutes DOUBLE PRECISION,
    noise_level_db DOUBLE PRECISION,
    stealth_rating DOUBLE PRECISION,
    weather_resistance VARCHAR(50),
    max_wind_resistance_ms DOUBLE PRECISION,
    surveillance_capability TEXT,
    applications TEXT,
    sort_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sensor_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cart_id UUID NOT NULL REFERENCES nest_cart(id),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    boom_stress DOUBLE PRECISION NOT NULL,
    basket_sway DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    observation_distance DOUBLE PRECISION NOT NULL,
    wind_speed DOUBLE PRECISION DEFAULT 0.0,
    wind_direction DOUBLE PRECISION DEFAULT 0.0,
    temperature DOUBLE PRECISION DEFAULT 20.0
);

CREATE TABLE IF NOT EXISTS alert_record (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cart_id UUID NOT NULL REFERENCES nest_cart(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    message TEXT NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS terrain_elevation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_x INTEGER NOT NULL,
    grid_y INTEGER NOT NULL,
    elevation DOUBLE PRECISION NOT NULL,
    resolution DOUBLE PRECISION NOT NULL DEFAULT 10.0,
    region_name VARCHAR(100),
    UNIQUE(grid_x, grid_y, region_name)
);

CREATE TABLE IF NOT EXISTS vision_analysis_result (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cart_id UUID NOT NULL REFERENCES nest_cart(id),
    height DOUBLE PRECISION NOT NULL,
    visible_points INTEGER NOT NULL,
    total_points INTEGER NOT NULL,
    coverage_ratio DOUBLE PRECISION NOT NULL,
    max_visible_distance DOUBLE PRECISION NOT NULL,
    visible_grid JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sensor_data_cart_time ON sensor_data(cart_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_timestamp ON sensor_data(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_brin_timestamp ON sensor_data USING BRIN (timestamp) WITH (pages_per_range = 32);
CREATE INDEX IF NOT EXISTS idx_sensor_data_height ON sensor_data(cart_id, height, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_alert_record_cart_time ON alert_record(cart_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_record_type ON alert_record(alert_type);
CREATE INDEX IF NOT EXISTS idx_alert_record_severity ON alert_record(severity, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_record_unacked ON alert_record(acknowledged) WHERE acknowledged = FALSE;
CREATE INDEX IF NOT EXISTS idx_alert_record_brin_created ON alert_record USING BRIN (created_at) WITH (pages_per_range = 32);

CREATE INDEX IF NOT EXISTS idx_terrain_region_xy ON terrain_elevation(region_name, grid_x, grid_y);
CREATE INDEX IF NOT EXISTS idx_terrain_elevation ON terrain_elevation(region_name, elevation);

CREATE INDEX IF NOT EXISTS idx_vision_cart_height ON vision_analysis_result(cart_id, height, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vision_visible_grid ON vision_analysis_result USING GIN (visible_grid);
CREATE INDEX IF NOT EXISTS idx_vision_brin_created ON vision_analysis_result USING BRIN (created_at) WITH (pages_per_range = 32);

CREATE INDEX IF NOT EXISTS idx_nest_cart_name ON nest_cart(name);

CREATE INDEX IF NOT EXISTS idx_dynasty_cart_order ON dynasty_cart(sort_order);
CREATE INDEX IF NOT EXISTS idx_dynasty_cart_name ON dynasty_cart(dynasty_name);

CREATE INDEX IF NOT EXISTS idx_drone_spec_order ON modern_drone_spec(sort_order);
CREATE INDEX IF NOT EXISTS idx_drone_spec_category ON modern_drone_spec(category);

ANALYZE sensor_data;
ANALYZE alert_record;
ANALYZE terrain_elevation;
ANALYZE vision_analysis_result;
ANALYZE nest_cart;

INSERT INTO nest_cart (id, name, description, boom_length, boom_cross_section_area, boom_moment_of_inertia, boom_elastic_modulus, basket_weight, base_height, max_height, stress_limit, sway_limit, crew_capacity, dynasty)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '巢车一号', '春秋时期复原巢车，主悬臂8米，吊篮承重150kg', 8.0, 0.01, 8.33e-6, 1.2e10, 150.0, 4.0, 15.0, 8.0e6, 0.5, 3, '春秋'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', '巢车二号', '战国改进型巢车，加强悬臂10米，吊篮承重200kg', 10.0, 0.015, 1.25e-5, 1.2e10, 200.0, 5.0, 18.0, 1.0e7, 0.4, 4, '战国');

INSERT INTO terrain_elevation (grid_x, grid_y, elevation, resolution, region_name)
SELECT
    x, y,
    50.0 + 30.0 * sin(radians(x * 3.6)) * cos(radians(y * 3.6))
     + 15.0 * sin(radians(x * 7.2 + 30)) * cos(radians(y * 5.4 + 45))
     + 5.0 * random(),
    10.0,
    'default_battlefield'
FROM generate_series(0, 99) AS x
CROSS JOIN generate_series(0, 99) AS y;

INSERT INTO dynasty_cart (dynasty_name, period, era_year, historical_context, boom_length, boom_cross_section_area, boom_moment_of_inertia, boom_elastic_modulus, basket_weight, base_height, max_height, stress_limit, sway_limit, crew_size, observation_distance_estimate, construction_material, military_role, historical_record, innovation_features, evolution_score, sort_order) VALUES
('西周', '公元前1046-前771年', '西周早期', '巢车雏形出现，作为瞭望塔附属于攻城部队，为古代巢车的萌芽阶段', 5.0, 0.006, 4.5e-6, 9.0e9, 80.0, 2.5, 8.0, 4.0e6, 0.8, 2, 8.0, '原木捆绑+绳索', '单纯瞭望、敌情观察', '《诗经·大雅》记载最早的登高观察记载', '首次将木塔与战车结合', 35.0, 1),
('春秋', '公元前770-前476年', '春秋中晚期', '诸侯争霸推动攻城器械发展，巢车形制逐步成熟', 8.0, 0.01, 8.33e-6, 1.0e10, 150.0, 4.0, 12.0, 6.0e6, 0.6, 3, 10.0, '榫卯木构', '瞭望指挥、旗语传令', '《左传》记载巢车用于城濮之战', '榫卯结构取代绳索绑扎', 55.0, 2),
('战国', '公元前475-前221年', '战国晚期', '百家争鸣推动工程技术发展，墨家机械学贡献最大', 10.0, 0.015, 1.25e-5, 1.1e10, 200.0, 5.0, 15.0, 8.0e6, 0.5, 4, 12.0, '硬木+铜箍加固', '侦察指挥、投石制导', '《墨子·备城门》详述巢车结构', '铜箍加固关键节点，可容纳多人', 72.0, 3),
('汉代', '公元前202-220年', '西汉-东汉', '冶铁技术进步，铁器开始用于机械', 11.0, 0.018, 1.6e-5, 1.2e10, 250.0, 5.5, 17.0, 1.0e7, 0.45, 5, 13.5, '硬木+铁件加固', '瞭望、火攻指挥、箭塔', '《后汉书》记载官渡之战用巢车', '铁制连接件大幅提升结构强度', 80.0, 4),
('唐代', '618-907年', '盛唐', '攻城器械达到高峰，云梯与巢车结合', 12.5, 0.022, 2.1e-5, 1.2e10, 300.0, 6.0, 20.0, 1.2e7, 0.4, 6, 15.0, '复合木构+铁铠包覆', '瞭望、火攻、弓弩射击平台', '《通典·兵典》记载巢车与云梯配合使用', '可升降吊篮设计出现，开始使用滑轮组', 88.0, 5),
('宋代', '960-1279年', '北宋-南宋', '中国古代军事工程技术巅峰', 14.0, 0.025, 2.8e-5, 1.3e10, 350.0, 7.0, 22.0, 1.5e7, 0.35, 7, 16.0, '复合木构+钢铁复合', '瞭望、指挥通讯、火力引导、侦察测绘', '《武经总要》详细记载巢车全套图纸', '多节伸缩式悬臂，钢铁复合结构', 94.0, 6),
('明代', '1368-1644年', '明初-明末', '火器时代初期，巢车与火器结合', 13.0, 0.028, 3.2e-5, 1.3e10, 400.0, 7.5, 20.0, 1.8e7, 0.38, 8, 15.5, '硬木+钢铁构件+防火层', '瞭望、火炮瞄准、火器射击平台、通讯中继', '《武备志》收录多种巢车变体', '加装防火层、望镜孔，开始配备望远镜雏形', 96.0, 7);

INSERT INTO modern_drone_spec (model_name, manufacturer, category, year_introduced, max_flight_altitude_m, max_ceiling_m, max_flight_range_km, flight_endurance_minutes, cruise_speed_kmh, max_speed_kmh, camera_resolution_mp, optical_zoom, thermal_camera, thermal_resolution_mp, thermal_sensitivity_mk, surveillance_radius_km, data_link_range_km, payload_capacity_kg, takeoff_weight_kg, unit_cost_usd, operating_cost_per_hour_usd, crew_required, setup_time_minutes, noise_level_db, stealth_rating, weather_resistance, max_wind_resistance_ms, surveillance_capability, applications, sort_order) VALUES
('Mavic 3 Enterprise', 'DJI', '消费级多旋翼', 2021, 6000.0, 6000.0, 30.0, 45.0, 50.0, 75.0, 20.0, 7.0, TRUE, 0.64, 50.0, 10.0, 15.0, 0.5, 1.0, 4500.0, 20.0, 1, 5.0, 72.0, 65.0, 'IP44', 12.0, '可见光+热成像，RTK厘米级定位', '快速侦察、搜救、测绘、警用', 1),
('Matrice 350 RTK', 'DJI', '工业级多旋翼', 2023, 7000.0, 7000.0, 40.0, 55.0, 60.0, 94.0, 20.0, 7.0, TRUE, 0.64, 50.0, 15.0, 20.0, 2.7, 6.5, 10000.0, 50.0, 2, 10.0, 78.0, 60.0, 'IP55', 15.0, '多传感器载荷：广角/变焦/热成像/激光测距', '专业测绘、电力巡检、搜救、消防', 2),
('Skydio X10', 'Skydio', '工业级多旋翼', 2023, 6000.0, 6000.0, 42.0, 52.0, 58.0, 90.0, 48.0, 10.0, TRUE, 0.64, 30.0, 15.0, 20.0, 3.5, 7.5, 15000.0, 75.0, 1, 8.0, 75.0, 70.0, 'IP55', 15.0, '6向避障、自动飞行、夜视、AI目标识别', '军事侦察、基础设施巡检、公共安全', 3),
('Anafi USA', 'Parrot', '军用级多旋翼', 2020, 6000.0, 6000.0, 50.0, 55.0, 55.0, 83.0, 21.0, 8.0, TRUE, 0.64, 40.0, 15.0, 50.0, 0.9, 1.5, 14000.0, 60.0, 2, 10.0, 70.0, 75.0, 'IP53', 14.0, '保密数据链、无中国产组件、热成像+可见光', '美军排级侦察、特种作战', 4),
('RQ-11 Raven', 'AeroVironment', '手抛固定翼', 2005, 4500.0, 4500.0, 15.0, 90.0, 48.0, 81.0, 0.3, 1.0, TRUE, 0.064, 80.0, 10.0, 15.0, 0.27, 1.9, 35000.0, 150.0, 2, 5.0, 60.0, 80.0, 'MIL-STD-810', 12.5, 'EO/IR双传感器、手抛发射、美军制式', '美军班组级侦察、前线ISR', 5),
('MQ-9 Reaper', 'General Atomics', '长航时中空无人机', 2007, 15240.0, 15240.0, 1850.0, 1740.0, 313.0, 482.0, 1.8, 30.0, TRUE, 0.64, 50.0, 300.0, 1850.0, 1700.0, 4760.0, 32000000.0, 3500.0, 5, 600.0, 95.0, 30.0, '全天候', 15.0, 'MTS-B多光谱瞄准系统、合成孔径雷达、电子侦察', '战略侦察、精确打击、长时间监控', 6);

ANALYZE dynasty_cart;
ANALYZE modern_drone_spec;
