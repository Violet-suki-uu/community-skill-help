#!/usr/bin/env node

import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DEFAULT_BASE_URL = "https://lizonghui.asia";
const DEFAULT_ITEMS_PER_CATEGORY = 20;
const PASSWORD = "Demo@20260514";

const args = parseArgs(process.argv.slice(2));
const baseUrl = normalizeBaseUrl(args.baseUrl || process.env.SEED_API_BASE_URL || DEFAULT_BASE_URL);
const itemsPerCategory = Number(args.count || process.env.SEED_ITEMS_PER_CATEGORY || DEFAULT_ITEMS_PER_CATEGORY);
const resetRequested = Boolean(args.reset || process.env.SEED_RESET === "true");
const runId = new Date().toISOString().replace(/[:.]/g, "-");
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const logDir = path.resolve(process.cwd(), "seed-logs");
const coverDir = path.join(scriptDir, "demo-assets", "category-covers-small");
const coverAssetsByPhone = {
  "13926050101": "tutoring.jpg",
  "13926050102": "repair.jpg",
  "13926050103": "design.jpg",
  "13926050104": "photography.jpg",
  "13926050105": "programming.jpg",
  "13926050106": "errand.jpg",
  "13926050107": "general.jpg",
};

const categories = [
  {
    category: "家教",
    nickname: "家教公司",
    phone: "13926050101",
    color: "#2563eb",
    accent: "#f97316",
    basePrice: 80,
    titles: [
      "小学数学一对一辅导",
      "初中英语语法提升",
      "高中物理专题精讲",
      "少儿硬笔书法启蒙",
      "中考数学冲刺规划",
      "高考英语阅读训练",
      "小学语文阅读写作",
      "初中化学基础巩固",
      "儿童口才表达训练",
      "成人英语口语陪练",
      "奥数思维兴趣课",
      "高中生物重点梳理",
      "作文素材积累课",
      "学习习惯陪跑服务",
      "日语入门发音课",
      "雅思口语模拟训练",
      "托福写作批改辅导",
      "钢琴基础陪练",
      "美术素描入门",
      "家庭作业托管辅导",
    ],
  },
  {
    category: "维修",
    nickname: "维修公司",
    phone: "13926050102",
    color: "#0f766e",
    accent: "#eab308",
    basePrice: 60,
    titles: [
      "水龙头漏水检修",
      "家用电路故障排查",
      "空调清洗保养",
      "洗衣机小故障维修",
      "电脑系统重装优化",
      "路由器网络调试",
      "门锁更换安装",
      "墙面补漆修复",
      "家具五金加固",
      "厨房下水疏通",
      "灯具安装更换",
      "热水器基础检修",
      "冰箱异响排查",
      "手机贴膜与小修",
      "打印机连接调试",
      "窗帘轨道安装",
      "插座开关更换",
      "纱窗修补更换",
      "卫生间防水检查",
      "家电日常维护",
    ],
  },
  {
    category: "设计",
    nickname: "设计公司",
    phone: "13926050103",
    color: "#7c3aed",
    accent: "#06b6d4",
    basePrice: 120,
    titles: [
      "品牌 Logo 快速设计",
      "电商主图视觉设计",
      "活动海报排版设计",
      "名片与门店物料设计",
      "PPT 商务模板美化",
      "简历版式优化设计",
      "小程序界面原型设计",
      "社媒封面图设计",
      "餐饮菜单视觉升级",
      "包装贴纸设计",
      "宣传单页设计",
      "企业画册排版",
      "课程封面设计",
      "展架易拉宝设计",
      "头像与形象插画",
      "UI 图标风格统一",
      "婚礼请柬设计",
      "店铺招牌方案",
      "作品集排版整理",
      "品牌色彩规范整理",
    ],
  },
  {
    category: "摄影",
    nickname: "摄影公司",
    phone: "13926050104",
    color: "#111827",
    accent: "#f43f5e",
    basePrice: 180,
    titles: [
      "个人写真半日拍摄",
      "证件照精修拍摄",
      "产品静物摄影",
      "探店短片拍摄",
      "毕业照跟拍服务",
      "亲子家庭纪实摄影",
      "宠物写真拍摄",
      "活动现场摄影",
      "民宿空间摄影",
      "餐饮菜品拍摄",
      "婚礼跟拍轻套餐",
      "企业形象照拍摄",
      "服装模特棚拍",
      "旅行随拍服务",
      "短视频脚本拍摄",
      "无人机航拍预约",
      "老照片翻拍修复",
      "社交头像精修拍摄",
      "直播间布光指导",
      "摄影后期调色",
    ],
  },
  {
    category: "编程",
    nickname: "编程公司",
    phone: "13926050105",
    color: "#0369a1",
    accent: "#22c55e",
    basePrice: 160,
    titles: [
      "个人网站搭建",
      "小程序页面开发",
      "数据表格自动化",
      "Python 入门辅导",
      "爬虫脚本定制",
      "Excel VBA 工具开发",
      "接口联调排错",
      "前端页面样式调整",
      "数据库查询优化",
      "服务器部署协助",
      "WordPress 插件配置",
      "企业官网维护",
      "后台管理页面开发",
      "Node.js 接口开发",
      "Java 作业辅导",
      "移动端 H5 活动页",
      "自动化测试脚本",
      "Git 使用指导",
      "代码审查与建议",
      "微信机器人配置",
    ],
  },
  {
    category: "跑腿",
    nickname: "跑腿公司",
    phone: "13926050106",
    color: "#dc2626",
    accent: "#14b8a6",
    basePrice: 25,
    titles: [
      "同城文件代送",
      "鲜花蛋糕代取",
      "排队取号服务",
      "超市采购配送",
      "药品代买代送",
      "宠物用品急送",
      "钥匙同城送达",
      "证件材料递交",
      "火车站接送协助",
      "快递打包寄送",
      "外卖补送服务",
      "生日惊喜代办",
      "打印装订代取",
      "医院陪诊取单",
      "二手物品面交",
      "校园资料代送",
      "临时采购帮办",
      "上门取件寄件",
      "活动物料搬运",
      "社区便民代办",
    ],
  },
  {
    category: "其他",
    nickname: "综合服务公司",
    phone: "13926050107",
    color: "#475569",
    accent: "#84cc16",
    basePrice: 50,
    titles: [
      "宠物喂养上门服务",
      "绿植养护指导",
      "收纳整理陪同",
      "简历面试辅导",
      "活动主持预约",
      "二手交易验货",
      "本地向导陪同",
      "老人手机教学",
      "儿童绘本共读",
      "家宴菜单规划",
      "运动陪练入门",
      "桌游规则讲解",
      "咖啡拉花体验课",
      "手工香薰制作",
      "新房入住清单规划",
      "社区活动策划",
      "普通话纠音陪练",
      "个人形象整理",
      "旅行路线建议",
      "生活效率工具配置",
    ],
  },
];

const locations = [
  { lng: 121.4737, lat: 31.2304, address: "上海市黄浦区人民广场", adcode: "310101", cityName: "上海市" },
  { lng: 121.4998, lat: 31.2397, address: "上海市浦东新区陆家嘴", adcode: "310115", cityName: "上海市" },
  { lng: 121.4452, lat: 31.2133, address: "上海市徐汇区徐家汇", adcode: "310104", cityName: "上海市" },
  { lng: 121.3817, lat: 31.1124, address: "上海市闵行区莘庄", adcode: "310112", cityName: "上海市" },
  { lng: 121.4896, lat: 31.4053, address: "上海市宝山区顾村", adcode: "310113", cityName: "上海市" },
];

main().catch(async (error) => {
  console.error(error);
  await mkdir(logDir, { recursive: true });
  await writeFile(path.join(logDir, `seed-error-${runId}.log`), `${error.stack || error.message || error}\n`, "utf8");
  process.exitCode = 1;
});

async function main() {
  if (!Number.isInteger(itemsPerCategory) || itemsPerCategory < 1 || itemsPerCategory > 20) {
    throw new Error("--count 必须是 1 到 20 之间的整数");
  }

  await mkdir(logDir, { recursive: true });

  const summary = {
    baseUrl,
    runId,
    startedAt: new Date().toISOString(),
    itemsPerCategory,
    resetRequested,
    accounts: [],
    uploads: [],
    deletedSkills: [],
    createdSkills: [],
    skippedSkills: [],
    failures: [],
  };

  for (const profile of categories) {
    console.log(`\n[${profile.category}] 准备账号 ${profile.phone}`);
    const account = {
      category: profile.category,
      nickname: profile.nickname,
      phone: profile.phone,
      password: PASSWORD,
    };
    summary.accounts.push(account);

    await registerIfNeeded(account, summary);
    const token = await login(account);
    const mine = await getMySkills(token);

    if (resetRequested) {
      const deleted = await deleteDemoSkills(token, mine, profile, summary);
      if (deleted > 0) {
        console.log(`[${profile.category}] 已清理旧演示技能 ${deleted} 条`);
      }
    }

    const imageUrl = await uploadCategoryImage(profile, token);
    summary.uploads.push({ category: profile.category, imageUrl });

    const currentMine = resetRequested ? [] : mine;
    const existingTitles = new Set(
      currentMine
        .filter((item) => item.category === profile.category)
        .map((item) => String(item.title || "").trim())
        .filter(Boolean),
    );

    for (let index = 0; index < itemsPerCategory; index += 1) {
      const title = profile.titles[index];
      if (existingTitles.has(title)) {
        console.log(`[${profile.category}] 已存在，跳过：${title}`);
        summary.skippedSkills.push({ category: profile.category, title });
        continue;
      }

      const skill = buildSkill(profile, title, index, imageUrl);
      try {
        const id = await createSkill(token, skill);
        console.log(`[${profile.category}] 已发布 #${id} ${title}`);
        summary.createdSkills.push({ id, category: profile.category, title, phone: profile.phone });
      } catch (error) {
        const failure = {
          category: profile.category,
          title,
          message: error.message,
        };
        console.error(`[${profile.category}] 发布失败：${title} - ${error.message}`);
        summary.failures.push(failure);
      }
    }
  }

  summary.finishedAt = new Date().toISOString();
  const logPath = path.join(logDir, `seed-demo-data-${runId}.json`);
  await writeFile(logPath, `${JSON.stringify(summary, null, 2)}\n`, "utf8");
  console.log(`\n完成。账号密码与发布明细已写入：${logPath}`);
  console.log(`新增 ${summary.createdSkills.length} 条，跳过 ${summary.skippedSkills.length} 条，失败 ${summary.failures.length} 条。`);
}

function buildSkill(profile, title, index, imageUrl) {
  const location = locations[index % locations.length];
  const price = profile.basePrice + ((index * 17 + profile.category.length * 9) % 80);
  return {
    title,
    description: `${profile.nickname}提供的${profile.category}演示服务：${title}。适合社区邻里展示、预约沟通和服务详情演示，价格与内容均为演示数据。`,
    category: profile.category,
    price,
    imageUrl,
    lng: Number((location.lng + (index % 4) * 0.003).toFixed(6)),
    lat: Number((location.lat + (index % 5) * 0.002).toFixed(6)),
    address: location.address,
    adcode: location.adcode,
    cityName: location.cityName,
  };
}

async function registerIfNeeded(account, summary) {
  const response = await postJson("/api/auth/register", {
    phone: account.phone,
    password: account.password,
    nickname: account.nickname,
  });

  if (response.code === 0) {
    console.log(`[${account.category}] 注册成功`);
    return;
  }

  const message = String(response.message || "");
  if (message.includes("已注册") || message.toLowerCase().includes("registered")) {
    console.log(`[${account.category}] 账号已存在，改为登录复用`);
    return;
  }

  summary.failures.push({ category: account.category, phone: account.phone, message });
  throw new Error(`注册失败：${account.phone} - ${message || "未知错误"}`);
}

async function login(account) {
  const response = await postJson("/api/auth/login", {
    phone: account.phone,
    password: account.password,
  });
  assertOk(response, `登录失败：${account.phone}`);
  if (typeof response.data !== "string" || !response.data) {
    throw new Error(`登录失败：${account.phone} 未返回 token`);
  }
  console.log(`[${account.category}] 登录成功`);
  return response.data;
}

async function getMySkills(token) {
  const response = await request("/api/skills/mine", {
    headers: authHeaders(token),
  });
  assertOk(response, "获取我的技能失败");
  return Array.isArray(response.data) ? response.data : [];
}

async function createSkill(token, skill) {
  const response = await postJson("/api/skills", skill, authHeaders(token));
  assertOk(response, `发布失败：${skill.title}`);
  return response.data;
}

async function deleteDemoSkills(token, skills, profile, summary) {
  const targets = skills.filter((item) => item.category === profile.category);
  let deleted = 0;
  for (const item of targets) {
    try {
      const response = await request(`/api/skills/${item.id}`, {
        method: "DELETE",
        headers: authHeaders(token),
      });
      assertOk(response, `删除失败：${item.title || item.id}`);
      deleted += 1;
      summary.deletedSkills.push({ id: item.id, category: item.category, title: item.title, phone: profile.phone });
    } catch (error) {
      summary.failures.push({
        category: profile.category,
        title: item.title,
        message: `delete failed: ${error.message}`,
      });
      throw error;
    }
  }
  return deleted;
}

async function uploadCategoryImage(profile, token) {
  const form = new FormData();
  const asset = await loadCoverAsset(profile);
  form.append("file", asset.blob, asset.filename);
  const response = await request("/api/upload/image", {
    method: "POST",
    headers: authHeaders(token),
    body: form,
  });
  assertOk(response, `上传图片失败：${profile.category}`);

  if (typeof response.data !== "string" || !response.data) {
    throw new Error(`上传图片失败：${profile.category} 未返回图片地址`);
  }
  console.log(`[${profile.category}] 图片已上传 ${response.data}`);
  return toAbsoluteImageUrl(response.data);
}

async function loadCoverAsset(profile) {
  const filename = coverAssetsByPhone[profile.phone];
  if (filename) {
    try {
      const buffer = await readFile(path.join(coverDir, filename));
      return {
        filename,
        blob: new Blob([buffer], { type: filename.endsWith(".jpg") ? "image/jpeg" : "image/png" }),
      };
    } catch (error) {
      console.warn(`[${profile.category}] PNG 封面读取失败，改用 SVG 兜底：${error.message}`);
    }
  }

  const svg = buildCoverSvg(profile);
  return {
    filename: `${profile.category}.svg`,
    blob: new Blob([svg], { type: "image/svg+xml" }),
  };
}

function buildCoverSvg(profile) {
  const safeCategory = escapeXml(profile.category);
  const safeNickname = escapeXml(profile.nickname);
  const artwork = getThemeArtwork(profile.category, profile.color, profile.accent);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800">
  <defs>
    <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0" stop-color="${profile.color}"/>
      <stop offset="1" stop-color="${profile.accent}"/>
    </linearGradient>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="18" stdDeviation="22" flood-color="#000" flood-opacity=".26"/>
    </filter>
  </defs>
  <rect width="1200" height="800" fill="url(#bg)"/>
  <path d="M0 610 C240 520 360 720 620 615 C850 520 950 590 1200 500 L1200 800 L0 800 Z" fill="#ffffff" opacity=".18"/>
  <circle cx="1010" cy="150" r="96" fill="#ffffff" opacity=".16"/>
  <circle cx="180" cy="640" r="72" fill="#ffffff" opacity=".14"/>
  <g filter="url(#shadow)">
    <rect x="110" y="135" width="980" height="530" rx="42" fill="#ffffff" opacity=".92"/>
  </g>
  ${artwork}
  <text x="600" y="385" text-anchor="middle" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="104" font-weight="800" fill="${profile.color}">${safeCategory}</text>
  <text x="600" y="495" text-anchor="middle" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="46" font-weight="700" fill="#1f2937">${safeNickname}</text>
  <text x="600" y="575" text-anchor="middle" font-family="Arial, 'Microsoft YaHei', sans-serif" font-size="32" fill="#4b5563">社区技能服务演示封面</text>
</svg>`;
}

function getThemeArtwork(category, color, accent) {
  const common = `stroke="${color}" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" fill="none"`;
  const accentFill = `fill="${accent}"`;
  const softFill = `fill="${color}" opacity=".12"`;

  if (category === "家教") {
    return `<g transform="translate(250 170)">
    <rect x="5" y="30" width="150" height="105" rx="12" ${common}/>
    <path d="M80 30v105M35 62h28M35 94h28M98 62h28M98 94h28" ${common} stroke-width="10"/>
    <path d="M210 135l72-72 36 36-72 72-52 16z" ${accentFill} opacity=".9"/>
    <path d="M282 63l34-34 36 36-34 34" ${common}/>
  </g>`;
  }

  if (category === "维修") {
    return `<g transform="translate(245 165)">
    <path d="M95 35a76 76 0 0 0 85 100l118 118a32 32 0 0 0 45-45L225 90a76 76 0 0 0-100-85l52 52-30 30z" ${common}/>
    <path d="M350 32l52 52-188 188-52-52z" ${accentFill} opacity=".9"/>
    <path d="M330 52l52 52M180 202l52 52" ${common}/>
  </g>`;
  }

  if (category === "设计") {
    return `<g transform="translate(235 160)">
    <path d="M60 220l120-120 42 42-120 120-62 20z" ${accentFill} opacity=".95"/>
    <path d="M180 100l42 42 38-38-42-42zM40 282l62-20" ${common}/>
    <path d="M325 70h105v235H325z" ${common}/>
    <path d="M325 115h44M325 160h28M325 205h44M325 250h28" ${common} stroke-width="10"/>
    <path d="M525 130c55 18 82 63 64 108-16 40-62 59-112 43-28-9-43-28-37-47 5-16 22-24 42-18 18 6 35-1 40-16 6-17-8-33-35-42z" ${softFill}/>
    <circle cx="500" cy="170" r="13" ${accentFill}/>
    <circle cx="548" cy="208" r="13" fill="${color}"/>
    <circle cx="486" cy="244" r="13" ${accentFill}/>
  </g>`;
  }

  if (category === "摄影") {
    return `<g transform="translate(260 170)">
    <rect x="70" y="70" width="390" height="220" rx="34" ${common}/>
    <path d="M150 70l34-48h145l34 48M410 108h40" ${common}/>
    <circle cx="265" cy="180" r="74" ${common}/>
    <circle cx="265" cy="180" r="34" ${accentFill} opacity=".9"/>
    <rect x="95" y="105" width="70" height="36" rx="12" ${accentFill} opacity=".9"/>
  </g>`;
  }

  if (category === "编程") {
    return `<g transform="translate(250 165)">
    <rect x="50" y="45" width="470" height="270" rx="28" ${common}/>
    <path d="M50 105h470M130 205l-58-45 58-45M440 115l58 45-58 45M255 240l65-140" ${common}/>
    <circle cx="95" cy="76" r="9" ${accentFill}/>
    <circle cx="130" cy="76" r="9" fill="${color}"/>
    <circle cx="165" cy="76" r="9" ${accentFill}/>
  </g>`;
  }

  if (category === "跑腿") {
    return `<g transform="translate(250 180)">
    <path d="M105 210h250l55-95h-92l-45-65H150l-45 160z" ${common}/>
    <circle cx="155" cy="240" r="38" ${accentFill} opacity=".9"/>
    <circle cx="355" cy="240" r="38" fill="${color}" opacity=".9"/>
    <path d="M65 105h78M45 150h78M25 195h78M410 115h75l35 65" ${common}/>
    <rect x="175" y="20" width="112" height="76" rx="12" ${accentFill} opacity=".88"/>
  </g>`;
  }

  return `<g transform="translate(275 170)">
    <rect x="85" y="85" width="320" height="190" rx="30" ${common}/>
    <path d="M145 85v-28h200v28M185 150h120M185 205h170" ${common}/>
    <circle cx="450" cy="120" r="42" ${accentFill} opacity=".9"/>
    <path d="M430 120h40M450 100v40" stroke="#fff" stroke-width="12" stroke-linecap="round"/>
  </g>`;
}

async function postJson(url, body, headers = {}) {
  return request(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: JSON.stringify(body),
  });
}

async function request(url, options = {}) {
  const fullUrl = `${baseUrl}${url}`;
  const response = await fetch(fullUrl, options);
  const text = await response.text();
  let payload;
  try {
    payload = text ? JSON.parse(text) : {};
  } catch {
    throw new Error(`${fullUrl} 返回非 JSON：HTTP ${response.status} ${text.slice(0, 160)}`);
  }

  if (!response.ok) {
    throw new Error(`${fullUrl} 请求失败：HTTP ${response.status} ${payload.message || response.statusText}`);
  }
  return payload;
}

function assertOk(response, prefix) {
  if (!response || response.code !== 0) {
    throw new Error(`${prefix} - ${response?.message || "未知错误"}`);
  }
}

function authHeaders(token) {
  return { Authorization: `Bearer ${token}` };
}

function toAbsoluteImageUrl(url) {
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return `${baseUrl}${url.startsWith("/") ? "" : "/"}${url}`;
}

function normalizeBaseUrl(value) {
  return String(value || DEFAULT_BASE_URL).replace(/\/+$/, "");
}

function parseArgs(argv) {
  const parsed = {};
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--base-url") parsed.baseUrl = argv[++index];
    if (arg === "--count") parsed.count = argv[++index];
    if (arg === "--reset") parsed.reset = true;
  }
  return parsed;
}

function escapeXml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
