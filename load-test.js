import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 5000, // 5000 requests
            timeUnit: '1s', // per 1 second
            duration: '10s', // sustain for 10 seconds (50,000 total requests)
            preAllocatedVUs: 1000, // Initial thread pool
            maxVUs: 5000, // Max threads to use if the backend gets slow
        },
    },
};

export default function () {
    const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEwMSIsInJvbGUiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODY3NzA3ODAsImV4cCI6MTc4NjgwNjc4MH0.pYjmqeLtLUoe73oQ3ZM_9u7i11ydCdZM4F2jby620UI';

    const params = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    const res = http.post('http://localhost:8081/api/v1/inventory/TCS_Interview/reserve?seats=1', null, params);

    check(res, {
        'status is 200 (Booked Successfully)': (r) => r.status === 200,
        'status is 409 (Blocked by Optimistic Lock)': (r) => r.status === 409,
        'status is 429 (Blocked by Rate Limit)': (r) => r.status === 429,
    });
}
