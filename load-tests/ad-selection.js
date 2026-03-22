import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const adSelectionDuration = new Trend('ad_selection_duration', true);

const GEOS = ['US', 'IN', 'UK', 'DE', 'FR', 'JP', 'BR', 'CA', 'AU', 'SG'];
const CATEGORIES = ['technology', 'finance', 'health', 'sports', 'entertainment', 'travel', 'food', 'education', 'automotive', 'fashion'];
const DEVICES = ['MOBILE', 'DESKTOP', 'TABLET'];

export const options = {
  scenarios: {
    sustained_load: {
      executor: 'constant-arrival-rate',
      rate: 1000,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_duration: ['p(50)<10', 'p(95)<25', 'p(99)<50'],
    errors: ['rate<0.01'],
  },
};

function randomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const geo = randomElement(GEOS);
  const category = randomElement(CATEGORIES);
  const device = randomElement(DEVICES);
  const userId = `user_${Math.floor(Math.random() * 10000)}`;

  const url = `http://localhost:8080/v1/ad?geo=${geo}&category=${category}&device=${device}&user_id=${userId}`;

  const res = http.get(url);

  adSelectionDuration.add(res.timings.duration);

  const success = check(res, {
    'status is 200 or 204': (r) => r.status === 200 || r.status === 204,
    'response time < 50ms': (r) => r.timings.duration < 50,
  });

  errorRate.add(!success);
}
