import http from "k6/http";
import { check } from "k6";

const baseUrl = (__ENV.BASE_URL || "http://host.docker.internal:8080").replace(/\/$/, "");
const phone = __ENV.PHONE || "";
const password = __ENV.PASSWORD || "";
const vus = Number(__ENV.VUS || 100);
const httpTimeout = __ENV.HTTP_TIMEOUT || "60s";

export const options = {
  scenarios: {
    s1: {
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
    "http_req_duration{name:create}": ["p(95)<10000"],
  },
};

export function setup() {
  if (!phone || !password) {
    throw new Error("PHONE и PASSWORD (роль WARD)");
  }
}

export default function () {
  const login = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ phone, password }),
    { headers: { "Content-Type": "application/json" }, timeout: httpTimeout }
  );
  if (!check(login, { "login 200": (r) => r.status === 200 })) {
    return;
  }
  const token = login.json("accessToken");
  if (!token) {
    return;
  }

  const body = JSON.stringify({
    description: `s1 ${Date.now()}`,
    contactPhone: "+79990001122",
    wardFirstName: "Иван",
    wardLastName: "Тестов",
    wardPatronymic: null,
    location: { latitude: 55.75, longitude: 37.61 },
  });

  http.post(`${baseUrl}/api/v1/requests`, body, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    tags: { name: "create" },
    timeout: httpTimeout,
  });
}
