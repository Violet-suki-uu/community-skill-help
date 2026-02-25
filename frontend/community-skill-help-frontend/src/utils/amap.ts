/**
 * 模块说明：地图 SDK 工具模块。作用：按需加载高德地图脚本并缓存加载状态。
 */
declare global {
  interface Window {
    AMap?: any;
    _AMapSecurityConfig?: {
      securityJsCode: string;
    };
  }
}

const SCRIPT_ID = "amap-js-sdk";
const AMAP_VERSION = "2.0";
const AMAP_KEY = String(import.meta.env.VITE_AMAP_KEY || "f3fa674b932c5ab996ea6fc3695830dc").trim();
const AMAP_SECURITY_JS_CODE = String(import.meta.env.VITE_AMAP_SECURITY_JS_CODE || "a7940c998bcc3853db2f9141f7d4c7d1").trim();
const AMAP_PLUGINS = ["AMap.Geolocation", "AMap.Geocoder", "AMap.AutoComplete", "AMap.PlaceSearch"].join(",");

let amapLoader: Promise<any> | null = null;

function scriptSrc() {
  const query = new URLSearchParams({
    v: AMAP_VERSION,
    key: AMAP_KEY,
    plugin: AMAP_PLUGINS,
  });
  return `https://webapi.amap.com/maps?${query.toString()}`;
}

export function loadAmapSdk(): Promise<any> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("AMap can only be loaded in browser"));
  }

  if (window.AMap) {
    return Promise.resolve(window.AMap);
  }

  if (amapLoader) {
    return amapLoader;
  }

  if (!AMAP_KEY) {
    return Promise.reject(new Error("Missing AMap key"));
  }

  amapLoader = new Promise((resolve, reject) => {
    if (AMAP_SECURITY_JS_CODE) {
      window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_JS_CODE };
    }

    const finish = () => {
      if (!window.AMap) {
        reject(new Error("AMap SDK loaded but window.AMap is unavailable"));
        return;
      }
      resolve(window.AMap);
    };

    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", finish, { once: true });
      existing.addEventListener("error", () => reject(new Error("Failed to load AMap SDK")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = scriptSrc();
    script.async = true;
    script.defer = true;
    script.onload = finish;
    script.onerror = () => reject(new Error("Failed to load AMap SDK"));
    document.head.appendChild(script);
  }).catch((error) => {
    amapLoader = null;
    throw error;
  });

  return amapLoader;
}

export function preloadAmapSdk() {
  if (typeof window === "undefined") return;
  void loadAmapSdk().catch(() => undefined);
}


