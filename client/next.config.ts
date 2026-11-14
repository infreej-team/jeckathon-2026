import type { NextConfig } from "next";

// 로컬 개발 서버(next dev)에서만 /api 요청을 Spring 서버로 넘긴다.
// 운영에서는 nginx가 /api를 server로 보내므로 이 설정을 쓰지 않는다.
const isDev = process.env.NODE_ENV === "development";
const apiOrigin = process.env.API_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Docker 이미지에 필요한 파일만 담도록 standalone 결과물을 만든다.
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async rewrites() {
    return isDev ? [{ source: "/api/:path*", destination: `${apiOrigin}/api/:path*` }] : [];
  },
};

export default nextConfig;
