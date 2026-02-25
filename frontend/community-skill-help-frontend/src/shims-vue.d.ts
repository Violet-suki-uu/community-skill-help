/**
 * 模块说明：类型声明模块。作用：让 TypeScript 正确识别 .vue 文件。
 */
declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

