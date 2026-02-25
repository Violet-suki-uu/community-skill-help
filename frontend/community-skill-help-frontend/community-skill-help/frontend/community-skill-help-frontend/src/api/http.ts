import axios from "axios";
import type { AxiosInstance } from "axios";

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";

const http: AxiosInstance = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status;
    if (status === 401) {
    localStorage.removeItem("token");
    // 不要强制去 /login，未登录也要能看主页
    if (window.location.pathname !== "/home") {
      window.location.href = "/home";
    }
}
    return Promise.reject(err);
  }
);


export default http;
