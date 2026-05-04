import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const wardPhone = __ENV.WARD_PHONE || "";
const wardPassword = __ENV.WARD_PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";

export const options = {
  scenarios: {
    s3: {
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
    "http_req_duration{name:cancel}": ["p(95)<10000"],
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
  if (!wardPhone || !wardPassword) {
    throw new Error("Нужны WARD_PHONE и WARD_PASSWORD");
  }
  return {};
}

export default function () {
  const loginRes = login(wardPhone, wardPassword);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = loginRes.json("accessToken");

  const createBody = JSON.stringify({
    description: `nfr03 ${exec.vu.idInTest}-${exec.vu.iterationInScenario}`,
    contactPhone: "+79990001122",
    wardFirstName: "Нагрузка",
    wardLastName: "Отмена",
    wardPatronymic: null,
    location: { latitude: 55.75, longitude: 37.61 },
  });
  const created = http.post(`${baseUrl}/api/v1/requests`, createBody, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: httpTimeout,
  });
  if (!check(created, { "create 201": (r) => r.status === 201 })) {
    return;
  }
  const requestId = created.json("id");

  const cancelled = http.post(`${baseUrl}/api/v1/requests/${requestId}/cancel`, null, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "cancel" },
    timeout: httpTimeout,
  });
  check(cancelled, { "cancel 200": (r) => r.status === 200 });
}
