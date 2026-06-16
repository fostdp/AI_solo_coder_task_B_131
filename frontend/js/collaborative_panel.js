const CollaborativePanel = (function() {
    const _delegate = typeof CoverageOptimizer !== 'undefined' ? CoverageOptimizer : null;

    function init() {
        if (_delegate && _delegate.init) {
            return _delegate.init();
        }
    }

    function destroy() {
        if (_delegate && _delegate.destroy) {
            return _delegate.destroy();
        }
    }

    function refresh() {
        if (_delegate && _delegate.refresh) {
            return _delegate.refresh();
        }
    }

    const meta = _delegate?.meta || {
        name: 'CollaborativePanel',
        version: '1.0.0',
        apiBase: 'http://localhost:8080/api/collaborative'
    };

    return {
        init,
        destroy,
        refresh,
        meta
    };
})();
