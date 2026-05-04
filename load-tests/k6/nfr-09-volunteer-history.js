import http from "k6/http";
import { check } from "k6";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const volunteerPhone = __ENV.VOLUNTEER_PHONE || "";
const volunteerPassword = __ENV.VOLUNTEER_PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";

export const options = {
  scenarios: {
    s9: {
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
    "http_req_duration{name:history}": ["p(95)<10000"],
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
  if (!volunteerPhone || !volunteerPassword) {
    throw new Error("Нужны VOLUNTEER_PHONE и VOLUNTEER_PASSWORD");
  }
  const r = loginRaw(volunteerPhone, volunteerPassword);
  if (r.status !== 200) {
    throw new Error(`VOLUNTEER login failed: HTTP ${r.status}`);
  }
  const token = r.json("accessToken");
  const me = http.get(`${baseUrl}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
    timeout: httpTimeout,
  });
  if (me.status !== 200) {
    throw new Error(`users/me failed: HTTP ${me.status}`);
  }
  return { volunteerId: me.json("id") };
}

export default function (data) {
  const loginRes = loginTagged(volunteerPhone, volunteerPassword);
  if (!check(loginRes, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = loginRes.json("accessToken");

  const url = `${baseUrl}/api/v1/volunteers/${data.volunteerId}/requests/history`;
  const hist = http.get(url, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "history" },
    timeout: httpTimeout,
  });
  check(hist, { "history 200": (r) => r.status === 200 });
}
