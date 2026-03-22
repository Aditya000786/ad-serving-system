import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const adLatency = new Trend('ad_latency', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const GEOS = ['US', 'GB', 'DE', 'FR', 'JP', 'BR', 'IN', 'CA', 'AU', 'MX'];
const CITIES = ['new_york', 'london', 'berlin', 'paris', 'tokyo', 'sao_paulo', 'mumbai', 'toronto', 'sydney', 'mexico_city'];
const CATEGORIES = ['sports', 'tech', 'finance', 'entertainment', 'news', 'travel', 'food', 'health'];
const DEVICES = ['mobile', 'desktop', 'tablet'];

export const options = {
  scenarios: {
    ramp_to_10k: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 500,
      maxVUs: 2000,
      stages: [
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 5000 },
        { duration: '30s', target: 10000 },
        { duration: '2m', target: 10000 },  // sustain 10K RPS
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(50)<10', 'p(95)<50', 'p(99)<100'],
    errors: ['rate<0.01'],
  },
};

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const params = {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'ad_selection' },
  };

  const url = `${BASE_URL}/v1/ad?geo=${pick(GEOS)}&city=${pick(CITIES)}&category=${pick(CATEGORIES)}&device=${pick(DEVICES)}&user_id=user_${Math.floor(Math.random() * 100000)}`;

  const res = http.get(url, params);

  adLatency.add(res.timings.duration);

  const ok = check(res, {
    'status is 200 or 204': (r) => r.status === 200 || r.status === 204,
    'response time < 100ms': (r) => r.timings.duration < 100,
  });

  errorRate.add(!ok);
}
