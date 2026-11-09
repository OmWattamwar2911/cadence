import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    transfers_load: {
      executor: 'constant-vus',
      vus: 10,
      duration: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // < 1% error rate
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export function setup() {
  const timestamp = Date.now();
  const senderEmail = `load-sender-${timestamp}@test.com`;
  const receiverEmail = `load-receiver-${timestamp}@test.com`;
  const password = 'Password123!';

  // 1. Register sender
  let res = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email: senderEmail, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'sender registered': (r) => r.status === 201 });

  // 2. Register receiver
  res = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email: receiverEmail, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'receiver registered': (r) => r.status === 201 });

  // 3. Login sender to obtain JWT token
  res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: senderEmail, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'sender logged in': (r) => r.status === 200 });
  const token = res.json('accessToken');

  // 4. Deposit 1,000,000 paise (₹10,000) into sender's wallet
  const depIdemKey = `dep-${timestamp}`;
  res = http.post(
    `${BASE_URL}/api/v1/wallet/deposit`,
    JSON.stringify({ amount: 1000000 }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'Idempotency-Key': depIdemKey,
      },
    }
  );
  check(res, { 'sender funded': (r) => r.status === 200 });

  return { token, receiverEmail, senderEmail };
}

export default function (data) {
  const uniqueKey = `k6-${__VU}-${__ITER}-${Date.now()}-${Math.random()}`;
  const payload = JSON.stringify({
    toEmail: data.receiverEmail,
    amount: 100, // 100 paise = ₹1.00
    reference: `k6-tx-${__VU}-${__ITER}`,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
      'Idempotency-Key': uniqueKey,
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/transfers`, payload, params);

  check(res, {
    'transfer status is 200': (r) => r.status === 200,
    'transfer completed': (r) => r.json('status') === 'COMPLETED',
  });
}

export function teardown(data) {
  // Check final sender wallet balance
  const res = http.get(`${BASE_URL}/api/v1/wallet`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  if (res.status === 200) {
    console.log(`[TEARDOWN] Final Sender Balance: ${res.json('formattedBalance')} (${res.json('balance')} paise)`);
  }
}
