import http from "k6/http";
import { check } from "k6";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const volunteerPhone = __ENV.VOLUNTEER_PHONE || "";
const volunteerPassword = __ENV.VOLUNTEER_PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";
const radiusKm = __ENV.RADIUS_KM || "1";

export const options = {
  scenarios: {
    s4: {
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
    "http_req_duration{name:nearby}": ["p(95)<10000"],
  },
};

function login(phone, password) {
  return http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ phone, password }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "login" },
      timeout: httpTimeout,
    }
  );
}

export function setup() {
  if (!volunteerPhone || !volunteerPassword) {
    throw new Error("Нужны VOLUNTEER_PHONE и VOLUNTEER_PASSWORD");
  }
  return {};
}

export default function () {
  const loginRes = login(volunteerPhone, volunteerPassword);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = loginRes.json("accessToken");

  const url = `${baseUrl}/api/v1/requests/nearby?latitude=55.75&longitude=37.61&radiusKm=${radiusKm}`;
  const nearby = http.get(url, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "nearby" },
    timeout: httpTimeout,
  });
  check(nearby, { "nearby 200": (r) => r.status === 200 });
}
