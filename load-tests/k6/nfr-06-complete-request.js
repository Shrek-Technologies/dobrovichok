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
    s6: {
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
    "http_req_duration{name:complete}": ["p(95)<10000"],
  },
};

function loginTagged(phone, password) {
  return http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ phone, password }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "login" }, timeout: httpTimeout }
  );
}

function loginRaw(phone, password) {
  return http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ phone, password }),
    { headers: { "Content-Type": "application/json" }, timeout: httpTimeout }
  );
}

export function setup() {
  if (!wardPhone || !wardPassword || !volunteerPhone || !volunteerPassword) {
    throw new Error("Нужны WARD_* и VOLUNTEER_*");
  }
  const r = loginRaw(volunteerPhone, volunteerPassword);
  if (r.status !== 200) {
    throw new Error(`VOLUNTEER login failed: HTTP ${r.status}`);
  }
  return { volunteerToken: r.json("accessToken") };
}

export default function (data) {
  const loginRes = loginTagged(wardPhone, wardPassword);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const wardToken = loginRes.json("accessToken");

  const createBody = JSON.stringify({
    description: `nfr06 ${exec.vu.idInTest}-${exec.vu.iterationInScenario}`,
    contactPhone: "+79990001122",
    wardFirstName: "Закрытие",
    wardLastName: "Тест",
    wardPatronymic: null,
    location: { latitude: 55.75, longitude: 37.61 },
  });
  const created = http.post(`${baseUrl}/api/v1/requests`, createBody, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${wardToken}`,
    },
    timeout: httpTimeout,
  });
  if (!check(created, { "create 201": (r) => r.status === 201 })) {
    return;
  }
  const requestId = created.json("id");

  const accepted = http.post(`${baseUrl}/api/v1/requests/${requestId}/accept`, null, {
    headers: { Authorization: `Bearer ${data.volunteerToken}` },
    timeout: httpTimeout,
  });
  if (!check(accepted, { "accept 200": (r) => r.status === 200 })) {
    return;
  }

  const done = http.post(`${baseUrl}/api/v1/requests/${requestId}/complete`, null, {
    headers: { Authorization: `Bearer ${wardToken}` },
    tags: { name: "complete" },
    timeout: httpTimeout,
  });
  check(done, { "complete 200": (r) => r.status === 200 });
}
