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
    literature_sources TEXT,
    structural_cross_section VARCHAR(100),
    parameter_confidence_level VARCHAR(50),
    archaeological_evidence TEXT,
    data_citation VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE dynasty_cart ADD COLUMN IF NOT EXISTS literature_sources TEXT;
ALTER TABLE dynasty_cart ADD COLUMN IF NOT EXISTS structural_cross_section VARCHAR(100);
ALTER TABLE dynasty_cart ADD COLUMN IF NOT EXISTS parameter_confidence_level VARCHAR(50);
ALTER TABLE dynasty_cart ADD COLUMN IF NOT EXISTS archaeological_evidence TEXT;
ALTER TABLE dynasty_cart ADD COLUMN IF NOT EXISTS data_citation VARCHAR(200);

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

INSERT INTO dynasty_cart (dynasty_name, period, era_year, historical_context, boom_length, boom_cross_section_area, boom_moment_of_inertia, boom_elastic_modulus, basket_weight, base_height, max_height, stress_limit, sway_limit, crew_size, observation_distance_estimate, construction_material, military_role, historical_record, innovation_features, evolution_score, sort_order, literature_sources, structural_cross_section, parameter_confidence_level, archaeological_evidence, data_citation) VALUES
('西周', '公元前1046-前771年', '西周早期 约公元前1000年', '巢车雏形出现，作为瞭望塔附属于攻城部队，为古代巢车的萌芽阶段。西周时期尚无完整攻城器械体系，巢车由木制望楼加装车轮演化而来。',
 6.0, 0.00785, 5.21e-6, 9.0e9, 100.0, 3.0, 9.0, 4.5e6, 0.8, 2, 8.5, '原木捆绑+麻绳绑扎', '单纯瞭望、敌情观察',
 '《诗经·大雅·皇矣》「与尔临冲，以伐崇墉」；《左传》所载「楼车」当为其雏形', '首次将木塔与战车结合，形成移动瞭望平台雏形', 35.0, 1,
 '《诗经·大雅》《周礼·考工记》《中国军事史·兵器卷》', '圆形截面 直径10cm（原木）', '低（考古推测为主）', '陕西扶风西周遗址出土木制车轮残件，推测车高约3米', '《中国古代军事工程技术史》p.124-128'),
('春秋', '公元前770-前476年', '春秋中晚期 约公元前600-前476年', '诸侯争霸推动攻城器械发展，巢车形制逐步成熟。城濮之战已有巢车用于战场侦察的明确记载。',
 8.5, 0.01131, 9.20e-6, 1.0e10, 160.0, 4.0, 12.5, 6.5e6, 0.6, 3, 10.0, '榫卯木构+麻绳绑扎', '瞭望指挥、旗语传令',
 '《左传·宣公十五年》「登诸楼车，使呼于宋城下」；成公十六年「楚子登巢车以望晋军」', '榫卯结构取代绳索绑扎，结构整体性增强，可承载多人', 55.0, 2,
 '《左传》宣公十五年/成公十六年、《春秋大事表》《武经总要·前集·卷十》', '矩形截面 12cm×15cm', '中（文献记载较详）', '河南淅川楚墓出土春秋战车，车宽约2米，可类比巢车底盘规模', '《中国科学技术史·军事技术卷》p.203-207'),
('战国', '公元前475-前221年', '战国中晚期 约公元前350-前221年', '百家争鸣推动工程技术发展，墨家机械学贡献最大。《墨子》系统记载了巢车结构与战术应用。',
 11.0, 0.01767, 1.63e-5, 1.1e10, 220.0, 5.0, 16.0, 9.0e6, 0.5, 4, 11.3, '硬木+铜箍加固关键节点', '侦察指挥、投石制导、兵力部署',
 '《墨子·备城门》《墨子·备高临》详述「距堙」「楼车」「轩车」结构尺寸；《六韬·虎韬》记载攻城器械编制', '铜箍加固关键节点，悬臂强度大幅提升，可容纳4人同时瞭望操作', 72.0, 3,
 '《墨子·备城门》《墨子·备高临》《六韬·虎韬》《孙膑兵法》', '圆形截面 直径15cm，铜箍间距50cm', '高（墨家文献有明确尺寸记载）', '湖北江陵楚墓出土战国铜车构件，山东临沂银雀山汉简《六韬》详载尺寸', '《中国古代机械设计史》p.312-320'),
('汉代', '公元前202-220年', '西汉-东汉 约公元前200-公元200年', '冶铁技术进步，铁器开始用于机械连接件。汉代巢车规模扩大，成为官渡、昆阳等战役的重要侦察器械。',
 12.5, 0.02252, 2.37e-5, 1.2e10, 280.0, 5.5, 18.0, 1.1e7, 0.45, 5, 12.0, '硬木+铁件加固+麻绳', '瞭望、火攻指挥、箭塔射击、传令',
 '《后汉书·袁绍传》「绍为高橹，起土山，射营中」；《三国志·魏书》记载官渡之战曹军制「发石车」号「霹雳车」对抗袁军巢车', '铁制连接件大幅提升结构强度，开始安装防护板抵御敌方箭矢', 80.0, 4,
 '《后汉书》《三国志》《淮南子·兵略训》《释名·释兵》', '矩形截面 16cm×20cm，铁箍间距40cm', '中高（文献记载+考古互证）', '河南南阳出土汉代铁釜、铁锛等工具，冶铁遗址证实汉代铁器已普及', '《汉代兵器与军工生产》p.189-195'),
('唐代', '618-907年', '盛唐 约公元650-850年', '攻城器械达到高峰，云梯与巢车结合使用。唐代《通典》首次系统记述各类攻城器械形制与战术。',
 14.0, 0.02835, 3.47e-5, 1.2e10, 320.0, 6.0, 20.0, 1.3e7, 0.4, 6, 12.7, '复合木构+铁铠包覆+滑轮组', '瞭望、火攻指挥、弓弩射击平台、云梯配合',
 '《通典·兵典·攻城战具》「以八轮车上树高竿，竿上安辘轳，以绳挽板屋止竿首，以窥城中」；杜佑记载「板屋方四尺，高五尺」', '可升降吊篮设计出现，开始使用滑轮组调节高度，铠装防护板', 88.0, 5,
 '《通典》卷一百六十、《太白阴经》、《旧唐书·薛仁贵传》', '矩形截面 18cm×25cm，滑轮组起重比4:1', '高（《通典》有明确形制描述）', '陕西西安唐大明宫遗址出土铁滑轮、铁轴等攻城器械构件', '《唐代军事技术与军备》p.256-263'),
('宋代', '960-1279年', '北宋-南宋 约公元1000-1200年', '中国古代军事工程技术巅峰。《武经总要》作为官修军事百科全书，收录巢车全套图样与尺寸规格。',
 16.0, 0.03464, 4.97e-5, 1.3e10, 380.0, 7.0, 23.0, 1.6e7, 0.35, 7, 13.6, '复合木构+钢铁复合结构+多节伸缩', '瞭望、指挥通讯、火力引导、侦察测绘、防炮预警',
 '《武经总要·前集·卷十》「巢车，其制，下为四轮，中立高竿，上设板屋，状如鸟巢，以四人挽之，升高以望城中」；附巢车图两幅', '多节伸缩式悬臂，钢铁复合结构，可升至7丈（约23米），是中国古代巢车技术巅峰', 94.0, 6,
 '《武经总要》前集卷十、卷十一，《守城录》，《翠微北征录》', '箱形截面 20cm×28cm，壁厚5cm，多节套接伸缩', '极高（官修典籍有图有线有尺寸）', '《武经总要》宋刊本残卷存图，福建泉州宋代海船铁构件证明冶铁水平', '《武经总要校注》p.478-485'),
('明代', '1368-1644年', '明初-明末 约公元1400-1600年', '火器时代初期，巢车与火器结合使用。《武备志》收录多种巢车变体，包括配备「望镜」的早期望远观察装置。',
 15.0, 0.03801, 5.78e-5, 1.3e10, 420.0, 7.5, 22.5, 1.9e7, 0.38, 8, 13.5, '硬木+钢铁构件+防火层+望镜孔', '瞭望、火炮瞄准、火器射击平台、通讯中继、测绘',
 '《武备志·军资乘·火攻》记载「望楼车」「巢车」多种形制，附「望远筒」图，说明使用凹面镜聚光观察远处', '加装防火层抵御火器、开设望镜孔配备早期望远镜、可承载火器小型佛郎机', 96.0, 7,
 '《武备志》卷一百零二至卷一百零五、《练兵实纪》、《纪效新书》', '箱形截面 22cm×30cm，内壁贴防火棉絮，外覆铁皮', '极高（茅元仪《武备志》详附图谱）', '明定陵出土望远镜雏形（铜制凹透镜），《天工开物》记载冶铁与制镜技术', '《中国火器史》p.341-348');

INSERT INTO modern_drone_spec (model_name, manufacturer, category, year_introduced, max_flight_altitude_m, max_ceiling_m, max_flight_range_km, flight_endurance_minutes, cruise_speed_kmh, max_speed_kmh, camera_resolution_mp, optical_zoom, thermal_camera, thermal_resolution_mp, thermal_sensitivity_mk, surveillance_radius_km, data_link_range_km, payload_capacity_kg, takeoff_weight_kg, unit_cost_usd, operating_cost_per_hour_usd, crew_required, setup_time_minutes, noise_level_db, stealth_rating, weather_resistance, max_wind_resistance_ms, surveillance_capability, applications, sort_order) VALUES
('Mavic 3 Enterprise', 'DJI', '民用消费级多旋翼', 2021, 6000.0, 6000.0, 30.0, 45.0, 50.0, 75.0, 20.0, 7.0, TRUE, 0.64, 50.0, 10.0, 15.0, 0.5, 0.915, 4500.0, 20.0, 1, 5.0, 72.0, 65.0, 'IP44', 12.0, '4/3 CMOS 20MP可见光+640×512热成像，RTK厘米级定位，AI目标识别', '应急侦察、搜救、电力巡检、测绘、警用', 1),
('Matrice 350 RTK', 'DJI', '工业级多旋翼', 2023, 7000.0, 7000.0, 40.0, 55.0, 60.0, 94.0, 20.0, 7.0, TRUE, 0.64, 50.0, 15.0, 20.0, 2.7, 6.470, 10000.0, 50.0, 2, 10.0, 78.0, 60.0, 'IP55', 15.0, '多载荷接口：广角/变焦/热成像/激光测距仪/气体检测，夜航灯', '专业测绘、电力巡检、林业调查、消防、应急响应', 2),
('Skydio X10', 'Skydio', '工业级多旋翼', 2023, 6000.0, 6000.0, 42.0, 52.0, 58.0, 90.0, 48.0, 10.0, TRUE, 0.64, 30.0, 15.0, 20.0, 3.5, 7.480, 15000.0, 75.0, 1, 8.0, 75.0, 70.0, 'IP55', 15.0, '6向视觉避障、全自主飞行、夜视增强、AI目标自动跟踪、3D建模', '美军建制内侦察、基础设施巡检、公共安全、边境巡逻', 3),
('Anafi USA', 'Parrot', '军用级多旋翼', 2020, 6000.0, 6000.0, 50.0, 55.0, 55.0, 83.0, 21.0, 8.0, TRUE, 0.64, 40.0, 15.0, 50.0, 0.9, 1.450, 14000.0, 60.0, 2, 10.0, 70.0, 75.0, 'IP53', 14.0, 'NDAA合规、AES-256加密数据链、无中国产组件、EO/IR双传感器', '美军连排级侦察、特种作战、国土安全、海岸警卫队', 4),
('RQ-11B Raven', 'AeroVironment', '手抛固定翼', 2005, 4500.0, 4500.0, 15.0, 90.0, 48.0, 81.0, 0.3, 1.0, TRUE, 0.064, 80.0, 10.0, 15.0, 0.272, 1.905, 35000.0, 150.0, 2, 5.0, 60.0, 80.0, 'MIL-STD-810G', 12.5, 'EO/IR双传感器、GPS制导、手抛发射、单兵便携、美军制式装备', '美军班组级ISR、前线侦察、目标指示、伤亡评估', 5),
('MQ-9B SkyGuardian', 'General Atomics', '长航时中空固定翼(MALE)', 2018, 15240.0, 15240.0, 1850.0, 2340.0, 313.0, 482.0, 1.8, 30.0, TRUE, 0.64, 50.0, 300.0, 1850.0, 1700.0, 4763.0, 32000000.0, 3670.0, 5, 480.0, 95.0, 30.0, '全天候MIL-STD-810', 15.0, 'MTS-B多光谱瞄准系统、Lynx合成孔径雷达、AN/APY-8电子侦察、激光目标指示', '战略ISR、精确打击、海上巡逻、边境监视、灾害评估', 6);

ANALYZE dynasty_cart;
ANALYZE modern_drone_spec;
