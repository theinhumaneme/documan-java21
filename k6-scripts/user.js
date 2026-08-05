import http from 'k6/http';
import { check, group } from 'k6';

// The previous script hit /api/v1/user/id?userId=1, which has never been a route, so every
// iteration measured a 404. These target the real paths.
const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const USER_ID = __ENV.USER_ID || 1;
const POST_ID = __ENV.POST_ID || 1;

export const options = {
    scenarios: {
        // Cached single-entity reads: should be served from Redis after the first hit.
        entity_reads: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
            exec: 'entityReads',
        },
        // Paginated collection reads, which bypass the cache and go to PostgreSQL.
        list_reads: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
            exec: 'listReads',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{scenario:entity_reads}': ['p(95)<50'],
        'http_req_duration{scenario:list_reads}': ['p(95)<200'],
    },
};

export function entityReads() {
    group('cached entity reads', () => {
        check(http.get(`${BASE}/api/v1/user?userId=${USER_ID}`), {
            'user 200': (r) => r.status === 200,
        });
        check(http.get(`${BASE}/api/v1/post?postId=${POST_ID}`), {
            'post 200': (r) => r.status === 200,
        });
        check(http.get(`${BASE}/api/v1/department/all`), {
            'departments 200': (r) => r.status === 200,
        });
    });
}

export function listReads() {
    group('paginated reads', () => {
        check(http.get(`${BASE}/api/v1/post/all?page=0&size=20`), {
            'posts 200': (r) => r.status === 200,
            'posts paged': (r) => r.json('content') !== undefined,
        });
        check(http.get(`${BASE}/api/v1/comment/post?postId=${POST_ID}&page=0&size=20`), {
            'comments 200': (r) => r.status === 200,
        });
    });
}
