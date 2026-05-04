import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const wardPhone = __ENV.WARD_PHONE || "";
const wardPassword = __ENV.WARD_PASSWORD || "";
const volunteerPhone = __ENV.VOLUNTEER_PHONE || "";
const volunteerPassword = __ENV.VOLUNTEER_PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";

export const options = {
  scenarios: {
    s2: {
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
    "http_req_duration{name:accept}": ["p(95)<10000"],
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
  if (!wardPhone || !wardPassword || !volunteerPhone || !volunteerPassword) {
    throw new Error("Нужны WARD_* и VOLUNTEER_*");
  }
  const res = login(wardPhone, wardPassword);
  if (res.status !== 200) {
    throw new Error(`WARD login failed: HTTP ${res.status}`);
  }
  return { wardToken: res.json("accessToken") };
}

export default function (data) {
  const createBody = JSON.stringify({
    description: `nfr02 ${exec.vu.idInTest}-${exec.vu.iterationInScenario}`,
    contactPhone: "+79990001122",
    wardFirstName: "Нагрузка",
    wardLastName: "Тест",
    wardPatronymic: null,
    location: { latitude: 55.75, longitude: 37.61 },
  });
  const created = http.post(`${baseUrl}/api/v1/requests`, createBody, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${data.wardToken}`,
    },
    timeout: httpTimeout,
  });
  if (!check(created, { "create 201": (r) => r.status === 201 })) {
    return;
  }
  const requestId = created.json("id");

  const loginRes = login(volunteerPhone, volunteerPassword);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = loginRes.json("accessToken");

  const accepted = http.post(`${baseUrl}/api/v1/requests/${requestId}/accept`, null, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "accept" },
    timeout: httpTimeout,
  });
  check(accepted, { "accept 200": (r) => r.status === 200 });
}
