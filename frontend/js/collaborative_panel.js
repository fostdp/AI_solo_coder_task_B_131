const CollaborativePanel = (function() {
    const API_BASE = 'http://localhost:8080/api/collaborative';

    let currentResult = null;

    function init() {
        const btn = document.getElementById('runCollab');
        if (btn) btn.addEventListener('click', runOptimization);
        runOptimization();
    }

    async function runOptimization() {
        const w = parseFloat(document.getElementById('collabWidth')?.value || 10);
        const h = parseFloat(document.getElementById('collabHeight')?.value || 10);
        const n = parseInt(document.getElementById('collabCartCount')?.value || 3);
        const strategy = document.getElementById('collabStrategy')?.value || 'greedy_spread';

        const carts = [];
        for (let i = 0; i < n; i++) {
            const name = String.fromCharCode(65 + i);
            carts.push({
                cartName: `巢车-${name}`,
                x: w * (0.2 + 0.6 * Math.random()),
                y: h * (0.2 + 0.6 * Math.random()),
                height: 12 + Math.random() * 8,
                movable: true
            });
        }

        try {
            const res = await fetch(API_BASE + '/optimize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    region: 'default_battlefield',
                    regionWidthKm: w,
                    regionHeightKm: h,
                    strategy,
                    maxIterations: 300,
                    carts
                })
            });
            currentResult = await res.json();
            renderHeatmap();
            renderMetrics();
            renderBlindZones();
        } catch (e) {
            console.error(e);
        }
    }

    function renderHeatmap() {
        const canvas = document.getElementById('collabCoverageCanvas');
        if (!canvas || !currentResult) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const result = currentResult;
        const wKm = result.regionWidthKm || 10;
        const hKm = result.regionHeightKm || 10;
        const pad = 40;
        const cw = W - pad * 2, ch = H - pad * 2;
        const sx = cw / wKm, sy = ch / hKm;

        const gridRes = 50;
        const cellW = cw / gridRes, cellH = ch / gridRes;

        for (let i = 0; i < gridRes; i++) {
            for (let j = 0; j < gridRes; j++) {
                const px = (i + 0.5) / gridRes * wKm;
                const py = (j + 0.5) / gridRes * hKm;
                let count = 0;
                (result.placements || []).forEach(p => {
                    const dx = px - p.x, dy = py - p.y;
                    if (Math.sqrt(dx * dx + dy * dy) <= p.visionRadius) count++;
                });
                let color;
                if (count === 0) color = 'rgba(30,41,59,0.8)';
                else if (count === 1) color = 'rgba(6,182,212,0.35)';
                else if (count === 2) color = 'rgba(16,185,129,0.55)';
                else color = 'rgba(59,130,246,0.75)';
                ctx.fillStyle = color;
                ctx.fillRect(pad + i * cellW, pad + j * cellH, cellW + 1, cellH + 1);
            }
        }

        const colors = ['#ef4444', '#f59e0b', '#8b5cf6', '#10b981', '#06b6d4', '#ec4899', '#3b82f6', '#14b8a6'];
        (result.placements || []).forEach((p, i) => {
            const color = colors[i % colors.length];
            const cx = pad + p.x * sx;
            const cy = pad + p.y * sy;
            const rx = p.visionRadius * sx;
            const ry = p.visionRadius * sy;

            ctx.strokeStyle = color;
            ctx.fillStyle = color.replace(')', ',0.08)').replace('rgb', 'rgba');
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2);
            ctx.stroke();

            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.arc(cx, cy, 7, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#0a0e17';
            ctx.lineWidth = 2;
            ctx.stroke();

            ctx.fillStyle = '#e8ecf1';
            ctx.font = 'bold 12px sans-serif';
            ctx.textAlign = 'left';
            ctx.fillText(p.cartName || `#${i + 1}`, cx + 10, cy - 8);
            ctx.fillStyle = '#8b95a5';
            ctx.font = '10px sans-serif';
            ctx.fillText(`R=${p.visionRadius?.toFixed(1)}km`, cx + 10, cy + 6);
        });

        ctx.strokeStyle = '#2d3a4a';
        ctx.lineWidth = 1;
        ctx.strokeRect(pad, pad, cw, ch);

        ctx.fillStyle = '#8b95a5';
        ctx.font = '11px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(`战场 ${wKm}km × ${hKm}km`, pad + cw / 2, H - 12);
        ctx.save();
        ctx.translate(14, pad + ch / 2);
        ctx.rotate(-Math.PI / 2);
        ctx.fillText('南北 (km)', 0, 0);
        ctx.restore();

        const legend = [
            { c: 'rgba(30,41,59,0.8)', t: '盲区' },
            { c: 'rgba(6,182,212,0.5)', t: '1车覆盖' },
            { c: 'rgba(16,185,129,0.65)', t: '2车重叠' },
            { c: 'rgba(59,130,246,0.85)', t: '3+车重叠' }
        ];
        let lx = W - 140, ly = 12;
        legend.forEach(l => {
            ctx.fillStyle = l.c;
            ctx.fillRect(lx, ly, 14, 14);
            ctx.fillStyle = '#e8ecf1';
            ctx.font = '11px sans-serif';
            ctx.textAlign = 'left';
            ctx.fillText(l.t, lx + 20, ly + 12);
            ly += 20;
        });
    }

    function renderMetrics() {
        const el = document.getElementById('collabMetrics');
        if (!el || !currentResult) return;
        const r = currentResult;
        const m = r.optimizationMetrics || {};
        const cards = [
            { label: '战场总面积', value: `${r.totalAreaSqKm?.toFixed(1)} km²`, cls: '' },
            { label: '覆盖面积', value: `${r.coveredAreaSqKm?.toFixed(1)} km²`, cls: '' },
            { label: '覆盖率', value: `${(r.coverageRatio * 100).toFixed(1)}%`,
              cls: r.coverageRatio > 0.8 ? 'good' : r.coverageRatio > 0.5 ? 'warn' : 'bad' },
            { label: '重叠面积', value: `${r.overlapAreaSqKm?.toFixed(1)} km²`, cls: '' },
            { label: '重叠率', value: `${(r.overlapRatio * 100).toFixed(1)}%`, cls: '' },
            { label: '巢车数量', value: `${r.cartCount} 辆`, cls: '' },
            { label: '优化迭代', value: `${m.iterations || 0} 次`, cls: '' },
            { label: '覆盖率提升', value: `${((m.coverageImprovement || 0) * 100).toFixed(1)}%`,
              cls: (m.coverageImprovement || 0) > 0 ? 'good' : '' },
            { label: '计算耗时', value: `${m.computeTimeMs || 0} ms`, cls: '' }
        ];
        el.innerHTML = cards.map(c =>
            `<div class="metric-card"><div class="metric-label">${c.label}</div><div class="metric-value ${c.cls}">${c.value}</div></div>`
        ).join('');
    }

    function renderBlindZones() {
        const el = document.getElementById('collabBlindZones');
        if (!el || !currentResult) return;
        let html = '<h5>⚠️ 盲区与建议</h5><ul>';
        const zones = currentResult.blindZones || [];
        if (!zones.length) {
            html += '<li>当前部署已无明显盲区，协同覆盖效果良好。</li>';
        } else {
            zones.slice(0, 8).forEach(z => html += `<li>${z}</li>`);
            if (zones.length > 8) html += `<li>... 还有 ${zones.length - 8} 处盲区</li>`;
        }
        html += `<li>优化策略：${currentResult.optimizationMetrics?.strategy || '未知'}，建议在盲区附近增派巢车或提高现有巢车高度。</li>`;
        html += '</ul>';
        el.innerHTML = html;
    }

    return { init };
})();
