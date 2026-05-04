import http from "k6/http";
import { check } from "k6";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const phone = __ENV.PHONE || "";
const password = __ENV.PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";

export const options = {
  scenarios: {
    s8: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "5m", target: vus },
        { duration: "15m", target: vus },
        { duration: "1m", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.10"],
    "http_req_duration{name:login}": ["p(95)<10000"],
    "http_req_duration{name:me}": ["p(95)<10000"],
  },
};

function login(phone, password) {
  return http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ phone, password }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "login" }, timeout: httpTimeout }
  );
}

export function setup() {
  if (!phone || !password) {
    throw new Error("Нужны PHONE и PASSWORD");
  }
  return {};
}

export default function () {
  const loginRes = login(phone, password);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = loginRes.json("accessToken");

  const me = http.get(`${baseUrl}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "me" },
    timeout: httpTimeout,
  });
  check(me, { "me 200": (r) => r.status === 200 });
}
