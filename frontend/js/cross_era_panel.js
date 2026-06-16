const CrossEraPanel = (function() {
    const API_BASE = 'http://localhost:8080/api/comparison';

    let allDrones = [];
    let allCarts = [];
    let comparisonData = null;

    async function init() {
        try {
            const [dronesRes, cartsRes] = await Promise.all([
                fetch(API_BASE + '/drones'),
                fetch('http://localhost:8080/api/carts')
            ]);
            allDrones = await dronesRes.json();
            allCarts = await cartsRes.json();
            populateSelectors();
            await loadComparison();
        } catch (e) {
            console.error('Failed to load cross era data', e);
        }
    }

    function populateSelectors() {
        const cartSel = document.getElementById('cartSelectCross');
        const droneSel = document.getElementById('droneSelectCross');
        if (cartSel) {
            allCarts.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.id;
                opt.textContent = c.name;
                cartSel.appendChild(opt);
            });
            cartSel.addEventListener('change', loadComparison);
        }
        if (droneSel) {
            allDrones.forEach(d => {
                const opt = document.createElement('option');
                opt.value = d.id;
                opt.textContent = d.model_name;
                droneSel.appendChild(opt);
            });
            droneSel.addEventListener('change', loadComparison);
        }
    }

    async function loadComparison() {
        try {
            const cartSel = document.getElementById('cartSelectCross');
            const droneSel = document.getElementById('droneSelectCross');
            let url = API_BASE + '/cross-era';
            if (cartSel?.value && droneSel?.value) {
                url = API_BASE + `/vs-drone?cartId=${cartSel.value}&droneId=${droneSel.value}`;
            }
            const res = await fetch(url);
            comparisonData = await res.json();
            renderRadar();
            renderBar();
            renderTable();
            renderInsights();
        } catch (e) {
            console.error(e);
        }
    }

    function renderRadar() {
        const canvas = document.getElementById('crossEraRadarCanvas');
        if (!canvas || !comparisonData) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const centerX = W / 2, centerY = H / 2;
        const radius = Math.min(W, H) / 2 - 80;

        const ancient = comparisonData.ancientSummary?.capabilityScores || {};
        const modern = comparisonData.modernSummary?.capabilityScores || {};
        const labels = Object.keys(ancient);
        const n = labels.length;

        if (!n) return;

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
            ctx.fillText(labels[i], x, y + 4);
        }

        function drawPolygon(scores, color, fillColor) {
            ctx.beginPath();
            for (let i = 0; i <= n; i++) {
                const label = labels[i % n];
                const val = (scores[label] || 0) / 100;
                const r = val * radius;
                const angle = -Math.PI / 2 + ((i % n) / n) * Math.PI * 2;
                const x = centerX + r * Math.cos(angle);
                const y = centerY + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.fillStyle = fillColor;
            ctx.fill();
            ctx.strokeStyle = color;
            ctx.lineWidth = 2;
            ctx.stroke();
        }

        drawPolygon(ancient, '#f59e0b', 'rgba(245,158,11,0.25)');
        drawPolygon(modern, '#06b6d4', 'rgba(6,182,212,0.25)');

        const legendY = 30;
        ctx.textAlign = 'left';
        ctx.font = '13px sans-serif';
        ctx.fillStyle = '#f59e0b';
        ctx.fillRect(30, legendY - 10, 14, 14);
        ctx.fillStyle = '#e8ecf1';
        ctx.fillText(comparisonData.ancientSummary?.platformName || '古代巢车', 50, legendY + 2);
        ctx.fillStyle = '#06b6d4';
        ctx.fillRect(220, legendY - 10, 14, 14);
        ctx.fillStyle = '#e8ecf1';
        ctx.fillText(comparisonData.modernSummary?.platformName || '现代无人机', 240, legendY + 2);
    }

    function renderBar() {
        const canvas = document.getElementById('crossEraBarCanvas');
        if (!canvas || !comparisonData?.comparisonDimensions) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const dims = comparisonData.comparisonDimensions;
        const pad = 60;
        const chartW = W - pad * 2;
        const chartH = H - pad * 2;
        const barW = chartW / dims.length * 0.35;
        const gap = chartW / dims.length * 0.3;

        dims.forEach((d, i) => {
            const xBase = pad + i * (chartW / dims.length);
            const ancientScore = d.ancientScore || 0;
            const modernScore = d.modernScore || 0;

            const ancientH = (ancientScore / 100) * chartH;
            const modernH = (modernScore / 100) * chartH;

            ctx.fillStyle = '#f59e0b';
            ctx.fillRect(xBase, H - pad - ancientH, barW, ancientH);
            ctx.fillStyle = '#06b6d4';
            ctx.fillRect(xBase + barW + gap / 2, H - pad - modernH, barW, modernH);

            ctx.save();
            ctx.translate(xBase + barW, H - pad + 14);
            ctx.rotate(-Math.PI / 6);
            ctx.fillStyle = '#8b95a5';
            ctx.font = '11px sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText(d.dimension, 0, 0);
            ctx.restore();

            ctx.fillStyle = '#e8ecf1';
            ctx.font = '10px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(ancientScore.toFixed(0), xBase + barW / 2, H - pad - ancientH - 4);
            ctx.fillText(modernScore.toFixed(0), xBase + barW + gap / 2 + barW / 2, H - pad - modernH - 4);
        });

        ctx.fillStyle = '#8b95a5';
        ctx.font = '11px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillRect(30, 14, 12, 12);
        ctx.fillStyle = '#f59e0b';
        ctx.fillRect(30, 14, 12, 12);
        ctx.fillStyle = '#e8ecf1';
        ctx.fillText('古代巢车得分', 48, 24);
        ctx.fillStyle = '#06b6d4';
        ctx.fillRect(160, 14, 12, 12);
        ctx.fillStyle = '#e8ecf1';
        ctx.fillText('现代无人机得分', 178, 24);
    }

    function renderTable() {
        const container = document.getElementById('crossEraTableContainer');
        if (!container || !comparisonData?.comparisonDimensions) return;
        const dims = comparisonData.comparisonDimensions;

        let html = '<table><thead><tr><th>对比维度</th><th>类别</th><th>单位</th><th>古代巢车</th><th>现代无人机</th><th>优劣势</th><th>说明</th></tr></thead><tbody>';
        dims.forEach(d => {
            const fmt = v => typeof v === 'number' ? v.toFixed(2) : (v ?? '-');
            const advColor = d.advantage?.includes('无人机') ? '#06b6d4' :
                             d.advantage?.includes('巢车') ? '#f59e0b' : '#8b95a5';
            html += `<tr>
                <td><b>${d.dimension}</b></td>
                <td>${d.category || ''}</td>
                <td>${d.unit || ''}</td>
                <td>${fmt(d.ancientValue)}</td>
                <td>${fmt(d.modernValue)}</td>
                <td style="color:${advColor};font-weight:600;">${d.advantage || ''}</td>
                <td style="font-size:12px;color:#8b95a5;">${d.commentary || ''}</td>
            </tr>`;
        });
        html += '</tbody></table>';
        container.innerHTML = html;
    }

    function renderInsights() {
        const el = document.getElementById('crossEraInsights');
        if (!el || !comparisonData?.insights) return;
        let html = '<h5>🔍 跨时代洞察</h5><ul>';
        comparisonData.insights.forEach(t => html += `<li>${t}</li>`);
        html += '</ul>';
        el.innerHTML = html;
    }

    return { init };
})();
