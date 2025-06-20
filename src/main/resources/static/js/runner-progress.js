/**
 * Modern Runner Progress Component
 * Optimized for performance and maintainability
 */

// Constants and Configuration
const CONFIG = {
    MAX_DISTANCE: 500,
    API_ENDPOINT: '/running/runner-distances',
    RANK_COLORS: {
        0: '#FFD700', // Gold
        1: '#C0C0C0', // Silver
        2: '#CD7F32', // Bronze
        default: '#6b7280' // Gray
    },
    MEDAL_EMOJIS: {
        0: '🥇',
        1: '🥈', 
        2: '🥉',
        default: ''
    },
    ANIMATION_DURATION: 500,
    DEFAULT_PROFILE_IMAGE: '/images/runners/default.png'
};

// Utility Functions
const utils = {
    formatDistance: (distance) => distance.toFixed(1),
    
    getRankColor: (index) => CONFIG.RANK_COLORS[index] || CONFIG.RANK_COLORS.default,
    
    getMedalEmoji: (index) => CONFIG.MEDAL_EMOJIS[index] || CONFIG.MEDAL_EMOJIS.default,
    
    calculatePosition: (distance, maxDistance) => 
        Math.min(Math.max((distance / maxDistance) * 100, 0), 98),
    
    handleImageError: (event) => {
        event.target.onerror = null;
        event.target.src = CONFIG.DEFAULT_PROFILE_IMAGE;
    },

    debounce: (func, wait) => {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
};

// Data Management
class RunnerDataManager {
    constructor() {
        this.data = [];
        this.isLoading = false;
        this.error = null;
        this.cache = new Map();
        this.cacheTimeout = 5 * 60 * 1000; // 5 minutes
    }

    async fetchData() {
        const cacheKey = 'runnerData';
        const cached = this.cache.get(cacheKey);
        
        if (cached && Date.now() - cached.timestamp < this.cacheTimeout) {
            return cached.data;
        }

        this.isLoading = true;
        this.error = null;

        try {
            const response = await fetch(CONFIG.API_ENDPOINT);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: 데이터를 불러오는데 실패했습니다`);
            }
            
            const data = await response.json();
            
            // Sort data by distance (descending)
            const sortedData = data.sort((a, b) => b.totalDistance - a.totalDistance);
            
            // Cache the data
            this.cache.set(cacheKey, {
                data: sortedData,
                timestamp: Date.now()
            });
            
            this.data = sortedData;
            this.isLoading = false;
            
            // Dispatch custom event for stats update
            this.dispatchDataLoadedEvent(sortedData);
            
            return sortedData;
        } catch (error) {
            this.error = error.message;
            this.isLoading = false;
            console.error('Failed to fetch runner data:', error);
            throw error;
        }
    }

    dispatchDataLoadedEvent(data) {
        const event = new CustomEvent('runnerDataLoaded', { detail: data });
        window.dispatchEvent(event);
    }

    clearCache() {
        this.cache.clear();
    }
}

// Component Classes
class ProgressBar {
    static create(percentage) {
        return React.createElement('div', {
            className: 'progress-container',
            style: {
                position: 'relative',
                marginTop: '40px',
                marginBottom: '40px'
            }
        }, [
            React.createElement('div', {
                key: 'bar',
                className: 'progress-bar',
                style: {
                    position: 'relative',
                    width: '100%',
                    height: '30px',
                    backgroundColor: '#f1f5f9',
                    borderRadius: '15px',
                    overflow: 'hidden',
                    boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.1)'
                }
            }, React.createElement('div', {
                className: 'progress-fill',
                style: {
                    width: `${Math.min(percentage, 100)}%`,
                    height: '100%',
                    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
                    transition: `width ${CONFIG.ANIMATION_DURATION}ms cubic-bezier(0.4, 0, 0.2, 1)`,
                    borderRadius: '15px'
                }
            }))
        ]);
    }
}

class RunnerProfile {
    static create(runner, index, maxDistance) {
        const position = utils.calculatePosition(runner.totalDistance, maxDistance);
        const rankColor = utils.getRankColor(index);
        const medalEmoji = utils.getMedalEmoji(index);

        return React.createElement('div', {
            key: runner.name,
            className: 'runner-profile',
            style: {
                position: 'absolute',
                left: `${position}%`,
                top: '-35px',
                transform: 'translateX(-50%)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                zIndex: 10 - index, // Higher rank = higher z-index
                transition: `all ${CONFIG.ANIMATION_DURATION}ms ease-out`,
                animation: `slideInUp ${CONFIG.ANIMATION_DURATION}ms ease-out ${index * 100}ms both`
            }
        }, [
            React.createElement('div', {
                key: 'info',
                style: {
                    fontSize: '0.75rem',
                    marginBottom: '4px',
                    whiteSpace: 'nowrap',
                    color: rankColor,
                    backgroundColor: 'white',
                    padding: '4px 8px',
                    borderRadius: '8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                    border: `2px solid ${rankColor}`,
                    fontWeight: '600'
                }
            }, `${medalEmoji} ${runner.name} (${utils.formatDistance(runner.totalDistance)}km)`),
            
            React.createElement('img', {
                key: 'avatar',
                src: `/images/runners/${runner.name}.png`,
                alt: runner.name,
                style: {
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    border: `3px solid ${rankColor}`,
                    backgroundColor: 'white',
                    boxShadow: '0 4px 8px rgba(0,0,0,0.2)',
                    transition: 'transform 0.2s ease'
                },
                onError: utils.handleImageError,
                onMouseOver: (e) => e.target.style.transform = 'scale(1.1)',
                onMouseOut: (e) => e.target.style.transform = 'scale(1)'
            })
        ]);
    }
}

class RankingList {
    static create(runnerData) {
        if (!runnerData.length) {
            return React.createElement('div', {
                style: {
                    textAlign: 'center',
                    padding: '2rem',
                    color: '#64748b'
                }
            }, '기록이 없습니다');
        }

        return React.createElement('div', {
            style: {
                display: 'grid',
                gap: '0.75rem',
                padding: '1rem',
                backgroundColor: '#f8fafc',
                borderRadius: '12px',
                border: '1px solid #e2e8f0'
            }
        }, runnerData.map((runner, index) => this.createRankingItem(runner, index)));
    }

    static createRankingItem(runner, index) {
        const rankColor = utils.getRankColor(index);
        const medalEmoji = utils.getMedalEmoji(index);

        return React.createElement('div', {
            key: runner.name,
            className: 'ranking-item',
            style: {
                display: 'flex',
                alignItems: 'center',
                padding: '1rem',
                backgroundColor: 'white',
                borderRadius: '8px',
                boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
                border: `2px solid ${rankColor}`,
                transition: 'all 0.2s ease',
                animation: `fadeInUp ${CONFIG.ANIMATION_DURATION}ms ease-out ${index * 50}ms both`
            },
            onMouseOver: (e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 8px 16px rgba(0,0,0,0.1)';
            },
            onMouseOut: (e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.05)';
            }
        }, [
            React.createElement('div', {
                key: 'rank',
                style: {
                    fontSize: '1.25rem',
                    fontWeight: '700',
                    marginRight: '1rem',
                    color: rankColor,
                    minWidth: '40px',
                    textAlign: 'center'
                }
            }, `${medalEmoji}${index + 1}`),
            
            React.createElement('div', {
                key: 'runner-info',
                style: {
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.75rem',
                    flex: 1
                }
            }, [
                React.createElement('img', {
                    key: 'avatar',
                    src: `/images/runners/${runner.name}.png`,
                    alt: runner.name,
                    style: {
                        width: '48px',
                        height: '48px',
                        borderRadius: '50%',
                        border: `2px solid ${rankColor}`,
                        backgroundColor: 'white'
                    },
                    onError: utils.handleImageError
                }),
                React.createElement('span', {
                    key: 'name',
                    style: {
                        fontWeight: '600',
                        fontSize: '1rem',
                        color: '#1f2937'
                    }
                }, runner.name)
            ]),
            
            React.createElement('div', {
                key: 'distance',
                style: {
                    fontWeight: '700',
                    color: rankColor,
                    fontSize: '1.125rem',
                    marginLeft: 'auto'
                }
            }, `${utils.formatDistance(runner.totalDistance)}km`)
        ]);
    }
}

// Main Component
const RunnerProgress = () => {
    const [runnerData, setRunnerData] = React.useState([]);
    const [isLoading, setIsLoading] = React.useState(true);
    const [error, setError] = React.useState(null);
    
    const dataManager = React.useRef(new RunnerDataManager()).current;

    React.useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);
                const data = await dataManager.fetchData();
                setRunnerData(data);
                setError(null);
            } catch (err) {
                setError(err.message);
                setRunnerData([]);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();

        // Set up periodic refresh (every 5 minutes)
        const interval = setInterval(loadData, 5 * 60 * 1000);
        
        return () => {
            clearInterval(interval);
            dataManager.clearCache();
        };
    }, [dataManager]);

    // Loading State
    if (isLoading) {
        return React.createElement('div', {
            className: 'loading-state',
            style: {
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '1rem',
                padding: '3rem',
                color: '#64748b'
            }
        }, [
            React.createElement('div', {
                key: 'spinner',
                className: 'loading-spinner'
            }),
            React.createElement('div', {
                key: 'text'
            }, '데이터를 불러오는 중...')
        ]);
    }

    // Error State
    if (error) {
        return React.createElement('div', {
            className: 'error-state',
            style: {
                background: '#fee2e2',
                color: '#dc2626',
                padding: '1rem',
                borderRadius: '8px',
                textAlign: 'center',
                border: '1px solid #fecaca'
            }
        }, [
            React.createElement('div', {
                key: 'title',
                style: { fontWeight: '600', marginBottom: '0.5rem' }
            }, '⚠️ 오류가 발생했습니다'),
            React.createElement('div', {
                key: 'message'
            }, error)
        ]);
    }

    const maxAchievedDistance = runnerData.length > 0 ? runnerData[0].totalDistance : 0;
    const progressPercentage = (maxAchievedDistance / CONFIG.MAX_DISTANCE) * 100;

    return React.createElement('div', { className: 'runner-card' }, [
        // Goal Information
        React.createElement('div', {
            key: 'goal-info',
            style: {
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '1.5rem',
                padding: '1rem',
                backgroundColor: '#f1f5f9',
                borderRadius: '8px',
                border: '1px solid #e2e8f0'
            }
        }, [
            React.createElement('span', {
                key: 'goal',
                style: { fontWeight: '600', color: '#475569' }
            }, `🎯 목표 거리: ${CONFIG.MAX_DISTANCE}km`),
            React.createElement('span', {
                key: 'progress',
                style: { 
                    fontWeight: '600', 
                    color: '#6366f1',
                    fontSize: '0.875rem'
                }
            }, `${utils.formatDistance(progressPercentage)}% 달성`)
        ]),

        // Progress Bar with Runner Profiles
        React.createElement('div', {
            key: 'progress-section'
        }, [
            ProgressBar.create(progressPercentage),
            // Runner profiles on the progress bar
            ...runnerData.map((runner, index) => 
                RunnerProfile.create(runner, index, CONFIG.MAX_DISTANCE)
            )
        ]),

        // Ranking List
        React.createElement('div', {
            key: 'ranking',
            style: { marginTop: '2rem' }
        }, RankingList.create(runnerData))
    ]);
};

// CSS Animations (inject into document)
const animations = `
    @keyframes fadeInUp {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    @keyframes slideInUp {
        from {
            opacity: 0;
            transform: translateX(-50%) translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateX(-50%) translateY(0);
        }
    }
`;

// Inject animations
if (!document.querySelector('#runner-animations')) {
    const style = document.createElement('style');
    style.id = 'runner-animations';
    style.textContent = animations;
    document.head.appendChild(style);
}

// Render Component
ReactDOM.render(
    React.createElement(RunnerProgress),
    document.getElementById('runner-progress-root')
);