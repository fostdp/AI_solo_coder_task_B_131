const DynastyPanel = (function() {
    const API_BASE = 'http://localhost:8080/api/dynasty';

    let allCarts = [];
    let evolutionData = null;

    async function init() {
        try {
            const [cartsRes, evoRes] = await Promise.all([
                fetch(API_BASE + '/carts'),
                fetch(API_BASE + '/evolution')
            ]);
            allCarts = await cartsRes.json();
            evolutionData = await evoRes.json();
            renderTimeline();
            renderRadar();
            renderTable();
            renderInsights();
        } catch (e) {
            console.error('Failed to load dynasty data', e);
        }
    }

    function renderTimeline() {
        const canvas = document.getElementById('dynastyTimelineCanvas');
        if (!canvas || !allCarts.length) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const pad = 50;
        const innerH = H - pad * 2;
        const lineX = pad;

        ctx.strokeStyle = '#2d3a4a';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(lineX, pad);
        ctx.lineTo(lineX, H - pad);
        ctx.stroke();

        const colors = ['#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

        allCarts.forEach((cart, i) => {
            const y = pad + (i / Math.max(1, allCarts.length - 1)) * innerH;
            const color = colors[i % colors.length];

            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.arc(lineX, y, 10, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#0a0e17';
            ctx.lineWidth = 2;
            ctx.stroke();

            ctx.fillStyle = color;
            ctx.font = 'bold 14px sans-serif';
            ctx.textAlign = 'left';
            ctx.fillText(cart.dynasty_name, lineX + 25, y - 18);

            ctx.fillStyle = '#8b95a5';
            ctx.font = '11px sans-serif';
            ctx.fillText(cart.period || '', lineX + 25, y - 4);

            ctx.fillStyle = '#e8ecf1';
            ctx.font = '12px sans-serif';
            ctx.fillText(`高度${cart.max_height?.toFixed(1)}m  评分${cart.evolution_score?.toFixed(0)}`, lineX + 25, y + 14);
        });
    }

    function renderRadar() {
        const canvas = document.getElementById('dynastyRadarCanvas');
        if (!canvas || !allCarts.length) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const centerX = W / 2, centerY = H / 2;
        const radius = Math.min(W, H) / 2 - 70;

        const metrics = [
            { key: 'max_height', label: '最大高度', max: 30 },
            { key: 'boom_length', label: '悬臂长度', max: 20 },
            { key: 'basket_weight', label: '承重能力', max: 500 },
            { key: 'crew_size', label: '乘员容量', max: 10 },
            { key: 'evolution_score', label: '技术评分', max: 100 }
        ];
        const n = metrics.length;

        ctx.strokeStyle = '#2d3a4a';
        ctx.lineWidth = 1;
        for (let level = 1; level <= 5; level++) {
            ctx.beginPath();
            for (let i = 0; i <= n; i++) {
                const angle = -Math.PI / 2 + (i / n) * Math.PI * 2;
                const r = (radius * level) / 5;
                const x = centerX + r * Math.cos(angle);
                const y = centerY + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.stroke();
        }

        ctx.fillStyle = '#8b95a5';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'center';
        for (let i = 0; i < n; i++) {
            const angle = -Math.PI / 2 + (i / n) * Math.PI * 2;
            const x = centerX + (radius + 30) * Math.cos(angle);
            const y = centerY + (radius + 30) * Math.sin(angle);
            ctx.fillText(metrics[i].label, x, y + 4);
        }

        const colors = ['rgba(59,130,246,0.5)', 'rgba(6,182,212,0.5)', 'rgba(16,185,129,0.5)',
            'rgba(245,158,11,0.5)', 'rgba(239,68,68,0.5)', 'rgba(139,92,246,0.5)', 'rgba(236,72,153,0.5)'];

        allCarts.forEach((cart, ci) => {
            const color = colors[ci % colors.length];
            ctx.beginPath();
            for (let i = 0; i <= n; i++) {
                const idx = i % n;
                const metric = metrics[idx];
                const val = (cart[metric.key] || 0) / metric.max;
                const r = Math.min(1, Math.max(0, val)) * radius;
                const angle = -Math.PI / 2 + (idx / n) * Math.PI * 2;
                const x = centerX + r * Math.cos(angle);
                const y = centerY + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.fillStyle = color;
            ctx.fill();
            ctx.strokeStyle = color.replace('0.5', '1');
            ctx.lineWidth = 2;
            ctx.stroke();
        });

        const legendY = H - 24;
        ctx.textAlign = 'left';
        ctx.font = '11px sans-serif';
        let lx = 10;
        allCarts.forEach((cart, ci) => {
            const color = colors[ci % colors.length].replace('0.5', '1');
            ctx.fillStyle = color;
            ctx.fillRect(lx, legendY - 8, 12, 12);
            ctx.fillStyle = '#e8ecf1';
            ctx.fillText(cart.dynasty_name, lx + 16, legendY + 2);
            lx += ctx.measureText(cart.dynasty_name).width + 36;
        });
    }

    function renderTable() {
        const container = document.getElementById('dynastyTableContainer');
        if (!container || !evolutionData || !evolutionData.comparisonTable) return;

        const dynasties = allCarts.map(c => c.dynasty_name);
        let html = '<table><thead><tr><th>指标</th><th>单位</th>';
        dynasties.forEach(d => html += `<th>${d}</th>`);
        html += '</tr></thead><tbody>';

        evolutionData.comparisonTable.forEach(row => {
            html += `<tr><td><b>${row.metric}</b></td><td>${row.unit || ''}</td>`;
            dynasties.forEach(d => {
                const val = row.valuesByDynasty?.[d];
                const display = typeof val === 'number' ? val.toFixed(1) : (val != null ? val : '-');
                html += `<td>${display}</td>`;
            });
            html += '</tr>';
        });

        html += '</tbody></table>';
        container.innerHTML = html;
    }

    function renderInsights() {
        const el = document.getElementById('dynastyInsights');
        if (!el || !evolutionData) return;

        let html = '<h5>📜 演变趋势洞察</h5><ul>';
        (evolutionData.evolutionTrend || []).forEach(t => {
            html += `<li>${t}</li>`;
        });
        const s = evolutionData.performanceSummary || {};
        html += `<li>统计样本：共 ${s.dynastyCount || 0} 个朝代，平均高度 ${(s.avgHeight || 0).toFixed(1)} 米，平均技术评分 ${(s.avgEvolutionScore || 0).toFixed(0)} 分</li>`;
        html += '</ul>';
        el.innerHTML = html;
    }

    return { init };
})();
