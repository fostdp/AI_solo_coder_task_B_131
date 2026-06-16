const VirtualRide = (function() {
    let scene, camera, renderer;
    let terrain, cartGroup, basketGroup;
    let vignetteOverlay;
    let currentHeight = 0;
    let targetHeight = 0;
    let animating = false;
    let autoMode = false;
    let autoDir = 1;
    let mouseX = 0, mouseY = 0;
    let yaw = 0, pitch = 0;
    let smoothedYaw = 0, smoothedPitch = 0;
    let initialized = false;
    let pointerLocked = false;
    let acrophobiaProtection = false;
    let comfortMode = false;
    let maxHeightLimit = 22;
    let lastVignetteAlpha = 0;
    let currentSwayAngle = 0;
    let smoothedHeight = 0;
    const smoothFactor = 0.08;
    const yawSmooth = 0.15;
    const pitchSmooth = 0.15;

    const stories = [
        { h: 0, title: '春秋·城濮之战', content: '公元前632年，晋楚城濮之战。晋军斥候立于巢车之上，观察楚军阵势，见其军容不整，回报晋侯。晋文公遂下令进击，大败楚军，奠定晋国百年霸业。' },
        { h: 3, title: '战国·墨子守城', content: '墨家弟子善于造器，《墨子·备城门》记载：「楼若本于城，曰渠。」巢车作为移动渠楼，可快速部署于城门之外，观察敌军攻城器械动向。' },
        { h: 6, title: '秦·长平之战', content: '公元前260年，秦赵长平对峙。赵括登巢车望见白起旗号，误判秦军兵力，下令出击。秦将白起以两翼奇兵断绝赵军粮道，四十万赵卒尽被坑杀。' },
        { h: 10, title: '三国·官渡之战', content: '公元200年，曹操与袁绍相持于官渡。曹军巢车昼夜瞭望，发现袁军乌巢屯粮。曹操亲率奇兵夜袭乌巢，焚烧袁军粮草，袁绍百万之众遂土崩瓦解。' },
        { h: 15, title: '唐·虎牢关之战', content: '公元621年，李世民围攻洛阳王世充，窦建德率十万大军来援。世民登巢车远眺，见夏军队伍散漫，乃以精骑突击，生擒窦建德，一战定鼎中原。' },
        { h: 20, title: '宋·襄阳保卫战', content: '南宋末年，元军围攻襄阳六年。宋军以「望楼车」居高临下，观察元军「回回炮」动向，指挥城内军民避炮。然孤城无援，终为元军所破。' }
    ];

    function init() {
        const container = document.getElementById('virtualRideContainer');
        if (!container || initialized) return;
        initialized = true;

        const W = container.clientWidth || 800;
        const H = 600;

        scene = new THREE.Scene();
        scene.background = new THREE.Color(0x050810);
        scene.fog = new THREE.Fog(0x050810, 80, 500);

        camera = new THREE.PerspectiveCamera(70, W / H, 0.1, 2000);
        camera.position.set(0, 1.7, 0);
        camera.lookAt(0, 1.7, -10);

        renderer = new THREE.WebGLRenderer({ antialias: true });
        renderer.setSize(W, H);
        renderer.setPixelRatio(window.devicePixelRatio);
        renderer.shadowMap.enabled = true;
        container.appendChild(renderer.domElement);

        const hemiLight = new THREE.HemisphereLight(0x4a5568, 0x1a202c, 0.8);
        scene.add(hemiLight);
        const dirLight = new THREE.DirectionalLight(0xffd89b, 0.7);
        dirLight.position.set(50, 80, 30);
        dirLight.castShadow = true;
        dirLight.shadow.camera.left = -200;
        dirLight.shadow.camera.right = 200;
        dirLight.shadow.camera.top = 200;
        dirLight.shadow.camera.bottom = -200;
        scene.add(dirLight);

        buildTerrain();
        buildCart();
        buildBattlefield();
        buildSky();
        buildVignetteOverlay(container, W, H);

        renderer.domElement.addEventListener('click', () => {
            if (!pointerLocked) {
                renderer.domElement.requestPointerLock && renderer.domElement.requestPointerLock();
            }
        });
        document.addEventListener('pointerlockchange', () => {
            pointerLocked = document.pointerLockElement === renderer.domElement;
        });
        document.addEventListener('mousemove', onMouseMove);

        document.getElementById('rideAscend')?.addEventListener('click', () => {
            targetHeight = Math.min(maxHeightLimit, targetHeight + 5);
            animating = true;
            autoMode = false;
            setStatus('升空中...');
        });
        document.getElementById('rideDescend')?.addEventListener('click', () => {
            targetHeight = Math.max(0, targetHeight - 5);
            animating = true;
            autoMode = false;
            setStatus('下降中...');
        });
        document.getElementById('rideAuto')?.addEventListener('click', () => {
            autoMode = !autoMode;
            if (autoMode) {
                animating = true;
                autoDir = 1;
                setStatus('自动升降体验中...');
            } else {
                setStatus('已停止自动模式');
            }
        });
        document.getElementById('rideComfort')?.addEventListener('click', (e) => {
            comfortMode = !comfortMode;
            e.target.textContent = comfortMode ? '舒适模式：开' : '舒适模式：关';
            setStatus(comfortMode ? '舒适模式已开启（视角减速+画面稳定）' : '舒适模式已关闭');
        });
        document.getElementById('rideAcrophobia')?.addEventListener('click', (e) => {
            acrophobiaProtection = !acrophobiaProtection;
            e.target.textContent = acrophobiaProtection ? '恐高保护：开' : '恐高保护：关';
            if (acrophobiaProtection) {
                maxHeightLimit = 10;
                targetHeight = Math.min(targetHeight, 10);
                setStatus('恐高保护已开启（最高10米+视野黑边）');
            } else {
                maxHeightLimit = 22;
                setStatus('恐高保护已关闭');
            }
        });

        animate();
        window.addEventListener('resize', onResize);
    }

    function buildVignetteOverlay(container, W, H) {
        vignetteOverlay = document.createElement('canvas');
        vignetteOverlay.width = W;
        vignetteOverlay.height = H;
        Object.assign(vignetteOverlay.style, {
            position: 'absolute',
            top: '0',
            left: '0',
            width: '100%',
            height: '100%',
            pointerEvents: 'none',
            opacity: '0',
            transition: 'opacity 0.3s',
            zIndex: '10'
        });
        container.style.position = 'relative';
        container.appendChild(vignetteOverlay);
    }

    function updateVignette(height) {
        if (!vignetteOverlay) return;
        let alpha = 0;
        if (acrophobiaProtection) {
            alpha = Math.min(0.85, height / maxHeightLimit * 0.75);
        } else {
            alpha = Math.min(0.35, Math.max(0, height - 15) / 20);
        }
        if (comfortMode) alpha = Math.max(alpha, 0.15);
        alpha = lastVignetteAlpha + (alpha - lastVignetteAlpha) * 0.08;
        lastVignetteAlpha = alpha;

        const ctx = vignetteOverlay.getContext('2d');
        const W = vignetteOverlay.width;
        const H = vignetteOverlay.height;
        ctx.clearRect(0, 0, W, H);
        if (alpha > 0.001) {
            const grd = ctx.createRadialGradient(W/2, H/2, Math.min(W, H) * 0.2, W/2, H/2, Math.max(W, H) * 0.7);
            grd.addColorStop(0, `rgba(0,0,0,0)`);
            grd.addColorStop(0.6, `rgba(0,0,0,${alpha * 0.5})`);
            grd.addColorStop(1, `rgba(0,0,0,${alpha})`);
            ctx.fillStyle = grd;
            ctx.fillRect(0, 0, W, H);
        }
        vignetteOverlay.style.opacity = alpha > 0.001 ? '1' : '0';
    }

    function buildTerrain() {
        const size = 400, segments = 80;
        const geometry = new THREE.PlaneGeometry(size, size, segments, segments);
        geometry.rotateX(-Math.PI / 2);

        const pos = geometry.attributes.position;
        for (let i = 0; i < pos.count; i++) {
            const x = pos.getX(i);
            const z = pos.getZ(i);
            const h = 2 * Math.sin(x * 0.02) * Math.cos(z * 0.02)
                    + 1 * Math.sin(x * 0.05 + 1) * Math.cos(z * 0.04)
                    + 0.3 * Math.sin(x * 0.1) * Math.cos(z * 0.12);
            pos.setY(i, h);
        }
        geometry.computeVertexNormals();

        const colors = [];
        for (let i = 0; i < pos.count; i++) {
            const y = pos.getY(i);
            let r, g, b;
            if (y < 0) { r = 0.1; g = 0.25; b = 0.4; }
            else if (y < 1) { r = 0.2; g = 0.4; b = 0.15; }
            else if (y < 3) { r = 0.35; g = 0.45; b = 0.15; }
            else { r = 0.5; g = 0.45; b = 0.35; }
            colors.push(r, g, b);
        }
        geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));

        const material = new THREE.MeshStandardMaterial({
            vertexColors: true,
            flatShading: true,
            roughness: 0.95
        });
        terrain = new THREE.Mesh(geometry, material);
        terrain.receiveShadow = true;
        scene.add(terrain);
    }

    function buildCart() {
        cartGroup = new THREE.Group();

        const base = new THREE.Mesh(
            new THREE.BoxGeometry(5, 1, 4),
            new THREE.MeshStandardMaterial({ color: 0x4a3320, roughness: 0.85 })
        );
        base.position.y = 0.5;
        base.castShadow = true;
        cartGroup.add(base);

        for (let i = 0; i < 4; i++) {
            const wheel = new THREE.Mesh(
                new THREE.CylinderGeometry(0.7, 0.7, 0.4, 12),
                new THREE.MeshStandardMaterial({ color: 0x2a1810, roughness: 0.95 })
            );
            wheel.rotation.z = Math.PI / 2;
            wheel.position.set(i < 2 ? -2 : 2, 0.7, (i % 2 ? 1.5 : -1.5));
            wheel.castShadow = true;
            cartGroup.add(wheel);
        }

        const mast = new THREE.Mesh(
            new THREE.CylinderGeometry(0.2, 0.3, 25, 10),
            new THREE.MeshStandardMaterial({ color: 0x6b4423, roughness: 0.85 })
        );
        mast.position.y = 13;
        mast.castShadow = true;
        cartGroup.add(mast);

        for (let i = 0; i < 3; i++) {
            const brace = new THREE.Mesh(
                new THREE.CylinderGeometry(0.08, 0.08, 16, 6),
                new THREE.MeshStandardMaterial({ color: 0x5a3a1a, roughness: 0.95 })
            );
            const angle = (i / 3) * Math.PI * 2;
            brace.position.set(Math.cos(angle) * 5, 7, Math.sin(angle) * 5);
            brace.lookAt(0, 13, 0);
            brace.rotateX(Math.PI / 2);
            brace.scale.y = 16 / brace.geometry.parameters.height;
            cartGroup.add(brace);
        }

        basketGroup = new THREE.Group();
        const basket = new THREE.Mesh(
            new THREE.BoxGeometry(2.5, 1.5, 2.5),
            new THREE.MeshStandardMaterial({ color: 0x7a4a2a, roughness: 0.85, transparent: true, opacity: 0.88 })
        );
        basket.position.y = 0.75;
        basket.castShadow = true;
        basketGroup.add(basket);

        for (let i = 0; i < 8; i++) {
            const rail = new THREE.Mesh(
                new THREE.BoxGeometry(0.05, 1, 0.05),
                new THREE.MeshStandardMaterial({ color: 0x4a2a10 })
            );
            const angle = (i / 8) * Math.PI * 2;
            rail.position.set(Math.cos(angle) * 1.15, 1.5, Math.sin(angle) * 1.15);
            basketGroup.add(rail);
        }
        const railRing = new THREE.Mesh(
            new THREE.TorusGeometry(1.15, 0.04, 6, 12),
            new THREE.MeshStandardMaterial({ color: 0x4a2a10 })
        );
        railRing.rotation.x = Math.PI / 2;
        railRing.position.y = 2;
        basketGroup.add(railRing);

        const ropeMat = new THREE.LineBasicMaterial({ color: 0x8b6f47 });
        for (let i = 0; i < 4; i++) {
            const angle = (i / 4) * Math.PI * 2;
            const points = [
                new THREE.Vector3(Math.cos(angle) * 1.2, 2.5, Math.sin(angle) * 1.2),
                new THREE.Vector3(0, 25, 0)
            ];
            const rope = new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), ropeMat);
            basketGroup.add(rope);
        }

        basketGroup.position.y = 0;
        cartGroup.add(basketGroup);

        cartGroup.position.set(0, 0, 0);
        scene.add(cartGroup);
    }

    function buildBattlefield() {
        const armyMat = new THREE.MeshStandardMaterial({ color: 0x8b0000, roughness: 0.95 });
        for (let i = 0; i < 25; i++) {
            const angle = Math.random() * Math.PI * 2;
            const dist = 30 + Math.random() * 80;
            const soldier = new THREE.Mesh(
                new THREE.ConeGeometry(0.3, 1.8, 4),
                armyMat
            );
            soldier.position.set(Math.cos(angle) * dist, 0.9, Math.sin(angle) * dist);
            soldier.rotation.y = Math.atan2(-soldier.position.x, -soldier.position.z);
            soldier.castShadow = true;
            scene.add(soldier);
        }

        const flagMat = new THREE.MeshStandardMaterial({ color: 0xd4a017, side: THREE.DoubleSide });
        for (let i = 0; i < 5; i++) {
            const pole = new THREE.Mesh(
                new THREE.CylinderGeometry(0.05, 0.05, 8, 5),
                new THREE.MeshStandardMaterial({ color: 0x3a2010 })
            );
            const angle = (i / 5) * Math.PI * 2;
            const dist = 25 + Math.random() * 20;
            pole.position.set(Math.cos(angle) * dist, 4, Math.sin(angle) * dist);
            scene.add(pole);

            const flag = new THREE.Mesh(
                new THREE.PlaneGeometry(3, 2),
                flagMat
            );
            flag.position.set(pole.position.x + 1.5, pole.position.y + 2, pole.position.z);
            flag.lookAt(0, flag.position.y, 0);
            scene.add(flag);
        }

        const city = new THREE.Mesh(
            new THREE.BoxGeometry(40, 8, 40),
            new THREE.MeshStandardMaterial({ color: 0x5a5040, roughness: 0.95 })
        );
        city.position.set(-80, 4, -80);
        city.castShadow = true;
        city.receiveShadow = true;
        scene.add(city);

        const tower = new THREE.Mesh(
            new THREE.CylinderGeometry(3, 4, 20, 6),
            new THREE.MeshStandardMaterial({ color: 0x6a5a4a })
        );
        tower.position.set(-80, 14, -80);
        scene.add(tower);
    }

    function buildSky() {
        const stars = new THREE.BufferGeometry();
        const starPositions = [];
        for (let i = 0; i < 1500; i++) {
            const r = 800;
            const theta = Math.random() * Math.PI * 2;
            const phi = Math.random() * Math.PI * 0.5;
            starPositions.push(
                r * Math.sin(phi) * Math.cos(theta),
                r * Math.cos(phi) + 100,
                r * Math.sin(phi) * Math.sin(theta)
            );
        }
        stars.setAttribute('position', new THREE.Float32BufferAttribute(starPositions, 3));
        const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 1.2, sizeAttenuation: false });
        scene.add(new THREE.Points(stars, starMat));
    }

    function onMouseMove(e) {
        if (pointerLocked) {
            const sensitivity = comfortMode ? 0.001 : 0.002;
            yaw -= e.movementX * sensitivity;
            pitch -= e.movementY * sensitivity;
            pitch = Math.max(comfortMode ? -0.9 : -1.2, Math.min(comfortMode ? 0.6 : 0.8, pitch));
        } else {
            const rect = renderer?.domElement?.getBoundingClientRect();
            if (rect) {
                const range = comfortMode ? 0.3 : 0.5;
                mouseX = ((e.clientX - rect.left) / rect.width - 0.5) * 2 * range;
                mouseY = ((e.clientY - rect.top) / rect.height - 0.5) * 2 * range * 0.5;
            }
        }
    }

    function setStatus(s) {
        const el = document.getElementById('rideStatus');
        if (el) el.textContent = s;
    }

    function updateStory() {
        const el = document.getElementById('rideStory');
        if (!el) return;
        let active = stories[0];
        for (const s of stories) {
            if (currentHeight >= s.h) active = s;
        }
        el.innerHTML = `<div class="story-title">📍 ${active.title}（${active.h}米高度）</div>
                        <div class="story-content">${active.content}</div>`;
        const info = document.getElementById('rideSceneInfo');
        if (info) info.textContent = active.title;
    }

    function updateHUD() {
        const hEl = document.getElementById('rideHeight');
        const dEl = document.getElementById('rideDistance');
        const effH = comfortMode ? smoothedHeight * 0.85 : smoothedHeight;
        if (hEl) hEl.textContent = effH.toFixed(1);
        if (dEl) dEl.textContent = (Math.sqrt(2 * 6371000 * effH) / 1000).toFixed(2);
        const warn = document.getElementById('rideAcroWarning');
        if (warn) {
            warn.style.display = (acrophobiaProtection && smoothedHeight > 7) ? 'block' : 'none';
            if (acrophobiaProtection && smoothedHeight > 7) {
                warn.textContent = `恐高保护中 · 已限制高度 ≤ ${maxHeightLimit}m · 视野已收窄`;
            }
        }
    }

    function computeBasketSway(height) {
        const comfortDamping = comfortMode ? 0.2 : 1.0;
        const acroDamping = acrophobiaProtection ? 0.1 : 1.0;
        const naturalSway = Math.sin(Date.now() * 0.0005) * 0.008;
        const windGust = Math.sin(Date.now() * 0.00013) * 0.004 + Math.sin(Date.now() * 0.00031) * 0.002;
        const heightFactor = 1 + height * 0.015;
        return (naturalSway + windGust) * comfortDamping * acroDamping * heightFactor;
    }

    function animate() {
        requestAnimationFrame(animate);
        if (!renderer) return;

        if (autoMode) {
            const speed = comfortMode ? 0.015 : 0.03;
            targetHeight += autoDir * speed;
            if (targetHeight > maxHeightLimit) autoDir = -1;
            if (targetHeight < 0) autoDir = 1;
        }

        if (Math.abs(currentHeight - targetHeight) > 0.01) {
            const ascendSpeed = comfortMode ? 0.01 : 0.02;
            currentHeight += (targetHeight - currentHeight) * ascendSpeed;
        } else if (animating && !autoMode) {
            animating = false;
            setStatus('已到位');
        }

        smoothedHeight += (currentHeight - smoothedHeight) * smoothFactor;

        const targetSway = computeBasketSway(smoothedHeight);
        currentSwayAngle += (targetSway - currentSwayAngle) * 0.05;

        if (basketGroup) {
            basketGroup.position.y = 2 + smoothedHeight;
            basketGroup.rotation.z = currentSwayAngle;
            basketGroup.rotation.x = currentSwayAngle * 0.7;
        }

        smoothedYaw += (yaw - smoothedYaw) * yawSmooth;
        smoothedPitch += (pitch - smoothedPitch) * pitchSmooth;

        const camY = 2 + smoothedHeight + 1.7;
        camera.position.y = camY;

        if (pointerLocked) {
            const lookDist = comfortMode ? 15 : 20;
            camera.position.x = 0;
            camera.position.z = 0;
            camera.lookAt(
                Math.sin(smoothedYaw) * Math.cos(smoothedPitch) * lookDist,
                camY + Math.sin(smoothedPitch) * lookDist,
                -Math.cos(smoothedYaw) * Math.cos(smoothedPitch) * lookDist
            );
        } else {
            const baseYaw = -mouseX * 0.5;
            const basePitch = -mouseY * 0.2;
            const lookDist = 30;
            camera.position.x = 0;
            camera.position.z = 0;
            camera.lookAt(
                Math.sin(baseYaw) * Math.cos(basePitch) * lookDist,
                camY + Math.sin(basePitch) * lookDist,
                -Math.cos(baseYaw) * Math.cos(basePitch) * lookDist
            );
        }

        updateVignette(smoothedHeight);
        updateHUD();
        updateStory();
        renderer.render(scene, camera);
    }

    function onResize() {
        const container = document.getElementById('virtualRideContainer');
        if (!container || !renderer || !camera) return;
        const W = container.clientWidth || 800;
        const H = 600;
        camera.aspect = W / H;
        camera.updateProjectionMatrix();
        renderer.setSize(W, H);
        if (vignetteOverlay) {
            vignetteOverlay.width = W;
            vignetteOverlay.height = H;
        }
    }

    return { init };
})();
