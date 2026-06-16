const VirtualRide = (function() {
    let scene, camera, renderer;
    let terrain, cartGroup, basketGroup;
    let currentHeight = 0;
    let targetHeight = 0;
    let animating = false;
    let autoMode = false;
    let autoDir = 1;
    let mouseX = 0, mouseY = 0;
    let yaw = 0, pitch = 0;
    let initialized = false;
    let pointerLocked = false;

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
            targetHeight = Math.min(22, targetHeight + 5);
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

        animate();
        window.addEventListener('resize', onResize);
    }

    function buildTerrain() {
        const size = 400, segments = 120;
        const geometry = new THREE.PlaneGeometry(size, size, segments, segments);
        geometry.rotateX(-Math.PI / 2);

        const pos = geometry.attributes.position;
        for (let i = 0; i < pos.count; i++) {
            const x = pos.getX(i);
            const z = pos.getZ(i);
            const h = 2 * Math.sin(x * 0.02) * Math.cos(z * 0.02)
                    + 1 * Math.sin(x * 0.05 + 1) * Math.cos(z * 0.04)
                    + 0.5 * Math.random();
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
            roughness: 0.9
        });
        terrain = new THREE.Mesh(geometry, material);
        terrain.receiveShadow = true;
        scene.add(terrain);
    }

    function buildCart() {
        cartGroup = new THREE.Group();

        const base = new THREE.Mesh(
            new THREE.BoxGeometry(5, 1, 4),
            new THREE.MeshStandardMaterial({ color: 0x4a3320, roughness: 0.8 })
        );
        base.position.y = 0.5;
        base.castShadow = true;
        cartGroup.add(base);

        for (let i = 0; i < 4; i++) {
            const wheel = new THREE.Mesh(
                new THREE.CylinderGeometry(0.7, 0.7, 0.4, 16),
                new THREE.MeshStandardMaterial({ color: 0x2a1810, roughness: 0.9 })
            );
            wheel.rotation.z = Math.PI / 2;
            wheel.position.set(i < 2 ? -2 : 2, 0.7, (i % 2 ? 1.5 : -1.5));
            wheel.castShadow = true;
            cartGroup.add(wheel);
        }

        const mast = new THREE.Mesh(
            new THREE.CylinderGeometry(0.2, 0.3, 25, 12),
            new THREE.MeshStandardMaterial({ color: 0x6b4423, roughness: 0.8 })
        );
        mast.position.y = 13;
        mast.castShadow = true;
        cartGroup.add(mast);

        for (let i = 0; i < 3; i++) {
            const brace = new THREE.Mesh(
                new THREE.CylinderGeometry(0.08, 0.08, 16, 8),
                new THREE.MeshStandardMaterial({ color: 0x5a3a1a, roughness: 0.9 })
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
            new THREE.MeshStandardMaterial({ color: 0x7a4a2a, roughness: 0.8, transparent: true, opacity: 0.85 })
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
            new THREE.TorusGeometry(1.15, 0.04, 6, 16),
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
        const armyMat = new THREE.MeshStandardMaterial({ color: 0x8b0000, roughness: 0.9 });
        for (let i = 0; i < 30; i++) {
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
        for (let i = 0; i < 6; i++) {
            const pole = new THREE.Mesh(
                new THREE.CylinderGeometry(0.05, 0.05, 8, 6),
                new THREE.MeshStandardMaterial({ color: 0x3a2010 })
            );
            const angle = (i / 6) * Math.PI * 2;
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
            new THREE.MeshStandardMaterial({ color: 0x5a5040, roughness: 0.9 })
        );
        city.position.set(-80, 4, -80);
        city.castShadow = true;
        city.receiveShadow = true;
        scene.add(city);

        const tower = new THREE.Mesh(
            new THREE.CylinderGeometry(3, 4, 20, 8),
            new THREE.MeshStandardMaterial({ color: 0x6a5a4a })
        );
        tower.position.set(-80, 14, -80);
        scene.add(tower);
    }

    function buildSky() {
        const stars = new THREE.BufferGeometry();
        const starPositions = [];
        for (let i = 0; i < 2000; i++) {
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
        const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 1.5, sizeAttenuation: false });
        scene.add(new THREE.Points(stars, starMat));
    }

    function onMouseMove(e) {
        if (pointerLocked) {
            yaw -= e.movementX * 0.002;
            pitch -= e.movementY * 0.002;
            pitch = Math.max(-1.2, Math.min(0.8, pitch));
        } else {
            const rect = renderer?.domElement?.getBoundingClientRect();
            if (rect) {
                mouseX = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
                mouseY = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
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
        if (hEl) hEl.textContent = currentHeight.toFixed(1);
        if (dEl) dEl.textContent = (Math.sqrt(2 * 6371000 * currentHeight) / 1000).toFixed(2);
    }

    function animate() {
        requestAnimationFrame(animate);
        if (!renderer) return;

        if (autoMode) {
            targetHeight += autoDir * 0.03;
            if (targetHeight > 22) autoDir = -1;
            if (targetHeight < 0) autoDir = 1;
        }

        if (Math.abs(currentHeight - targetHeight) > 0.01) {
            currentHeight += (targetHeight - currentHeight) * 0.02;
        } else if (animating && !autoMode) {
            animating = false;
            setStatus('已到位');
        }

        if (basketGroup) {
            basketGroup.position.y = 2 + currentHeight;
            basketGroup.rotation.z = Math.sin(Date.now() * 0.0005) * 0.01;
        }

        const camY = 2 + currentHeight + 1.7;
        camera.position.y = camY;

        if (pointerLocked) {
            const lookDist = 20;
            camera.position.x = 0;
            camera.position.z = 0;
            camera.lookAt(
                Math.sin(yaw) * Math.cos(pitch) * lookDist,
                camY + Math.sin(pitch) * lookDist,
                -Math.cos(yaw) * Math.cos(pitch) * lookDist
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
    }

    return { init };
})();
