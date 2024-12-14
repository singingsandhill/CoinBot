const RunnerProgress = () => {
    const [runnerData, setRunnerData] = React.useState([]);
    const [isLoading, setIsLoading] = React.useState(true);
    const [error, setError] = React.useState(null);

    const MAX_DISTANCE = 500;

    React.useEffect(() => {
        fetch('/running/runner-distances')
            .then(response => {
                if (!response.ok) throw new Error('데이터를 불러오는데 실패했습니다');
                return response.json();
            })
            .then(data => {
                const sortedData = data.sort((a, b) => b.totalDistance - a.totalDistance);
                setRunnerData(sortedData);
                setIsLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setIsLoading(false);
            });
    }, []);

    if (isLoading) {
        return React.createElement('div', {
            style: {textAlign: 'center', padding: '2rem'}
        }, '로딩 중...');
    }

    if (error) {
        return React.createElement('div', {
            style: {textAlign: 'center', padding: '2rem', color: 'red'}
        }, error);
    }

    // 순위별 색상 설정
    const getRankColor = (index) => {
        switch(index) {
            case 0: return '#FFD700'; // 금메달 색상
            case 1: return '#C0C0C0'; // 은메달 색상
            case 2: return '#CD7F32'; // 동메달 색상
            default: return '#666666'; // 기본 색상
        }
    };

    return React.createElement('div', { className: 'runner-card' },
        React.createElement('h2', {
            style: {
                fontSize: '1.5rem',
                fontWeight: 'bold',
                marginBottom: '1.5rem',
                textAlign: 'center',
                padding: '0.5rem 0',
                borderBottom: '2px solid #eaeaea'  // 밑줄 효과 추가
            }
        }, '마일리지 현황'),
        React.createElement('div', {
            style: {marginBottom: '1rem'}
        }, `목표 거리: ${MAX_DISTANCE}km`),
        // 프로그레스바 컨테이너
        React.createElement('div', {
                className: 'progress-container',
                style: {
                    position: 'relative',
                    marginTop: '40px',
                    marginBottom: '40px'
                }
            },
            // 프로그레스바
            React.createElement('div', {
                    className: 'progress-bar',
                    style: {
                        position: 'relative',
                        width: '100%',
                        height: '30px',
                        backgroundColor: '#f0f0f0',
                        borderRadius: '10px',
                        overflow: 'hidden'
                    }
                },
                React.createElement('div', {
                    className: 'progress-fill',
                    style: {
                        width: `${Math.min((runnerData.reduce((acc, curr) => acc + curr.totalDistance, 0) / MAX_DISTANCE) * 100, 100)}%`,
                        height: '100%',
                        backgroundColor: '#4299e1',
                        transition: 'width 0.5s ease'
                    }
                })
            ),
            // 러너 프로필 이미지들
            ...runnerData.map((runner, index) => {
                const position = (runner.totalDistance / MAX_DISTANCE) * 100;
                return React.createElement('div', {
                        key: runner.name,
                        className: 'runner-profile',
                        style: {
                            position: 'absolute',
                            left: `${Math.min(Math.max(position, 0), 98)}%`,
                            top: '-30px',
                            transform: 'translateX(-50%)',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            zIndex: 2,
                        }
                    },
                    React.createElement('span', {
                        style: {
                            fontSize: '1.5rem',
                            marginBottom: '2px',
                            whiteSpace: 'nowrap',
                            color: getRankColor(index)
                        }
                    }, `${runner.name} (${runner.totalDistance.toFixed(1)}km)`),
                    React.createElement('img', {
                        src: `/images/runners/${runner.name}.png`,
                        alt: runner.name,
                        style: {
                            width: '35px',
                            height: '35px',
                            borderRadius: '50%',
                            border: `2px solid ${getRankColor(index)}`,
                            backgroundColor: 'white'
                        },
                        onError: (e) => {
                            e.target.onerror = null;
                            e.target.src = '/images/runners/default.png';
                        }
                    })
                );
            })
        ),
        // 총 진행 거리 표시
        // React.createElement('div', {
        //     style: {
        //         textAlign: 'right',
        //         marginBottom: '1rem',
        //         color: '#666'
        //     }
        // }, `총 진행 거리: ${runnerData.reduce((acc, curr) => acc + curr.totalDistance, 0).toFixed(1)}km`),
        // 순위 표시 부분
        React.createElement('div', {
                style: {
                    display: 'grid',
                    gap: '1rem',
                    padding: '1rem',
                    backgroundColor: '#f8f9fa',
                    borderRadius: '8px'
                }
            },
            runnerData.map((runner, index) =>
                React.createElement('div', {
                        key: runner.name,
                        style: {
                            display: 'flex',
                            alignItems: 'center',
                            padding: '0.5rem',
                            backgroundColor: 'white',
                            borderRadius: '6px',
                            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                            border: `2px solid ${getRankColor(index)}`
                        }
                    },
                    React.createElement('div', {
                        style: {
                            fontSize: '1.2rem',
                            fontWeight: 'bold',
                            marginRight: '1rem',
                            color: getRankColor(index),
                            width: '30px'
                        }
                    }, `${index + 1}`),
                    React.createElement('div', {
                            style: {
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.5rem',
                                flex: 1
                            }
                        },
                        React.createElement('img', {
                            src: `/images/runners/${runner.name}.png`,
                            alt: runner.name,
                            style: {
                                width: '40px',
                                height: '40px',
                                borderRadius: '50%',
                                border: `2px solid ${getRankColor(index)}`
                            },
                            onError: (e) => {
                                e.target.onerror = null;
                                e.target.src = '/images/runners/default.png';
                            }
                        }),
                        React.createElement('span', {
                            style: {
                                fontWeight: 'bold'
                            }
                        }, runner.name)
                    ),
                    React.createElement('div', {
                        style: {
                            fontWeight: 'bold',
                            color: getRankColor(index),
                            marginLeft: 'auto'
                        }
                    }, `${runner.totalDistance.toFixed(1)}km`)
                )
            )
        )
    );
};

// React 컴포넌트 렌더링
ReactDOM.render(
    React.createElement(RunnerProgress),
    document.getElementById('runner-progress-root')
);