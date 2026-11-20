// Local launcher only: the backend service itself runs entirely on Java.
const { spawn } = require("node:child_process");
const { existsSync, readFileSync } = require("node:fs");
const path = require("node:path");

const root = __dirname;
const env = { ...process.env };
const envFile = path.join(root, ".env");

if (existsSync(envFile)) {
  for (const line of readFileSync(envFile, "utf8").split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z_0-9]*)\s*=\s*(.*)\s*$/);
    if (!match || Object.prototype.hasOwnProperty.call(env, match[1])) continue;
    let value = match[2].trim();
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) value = value.slice(1, -1);
    env[match[1]] = value;
  }
}

if (!env.MONGO_URL || env.MONGO_URL.includes("mongodb://mongo:")) {
  env.MONGO_URL = "mongodb://127.0.0.1:27017/west_food";
}

if (!env.JWT_SECRET) {
  console.error("JWT_SECRET is missing. Set it in backend/.env or the environment.");
  process.exit(1);
}

// A system JDK is preferred. The second location is the temporary JDK used
// to validate this project on machines without Java installed in PATH.
const bundledJdk = path.join(require("node:os").tmpdir(), "westfood-java-tools");
if (!env.JAVA_HOME && existsSync(bundledJdk)) {
  const { readdirSync } = require("node:fs");
  const name = readdirSync(bundledJdk).find((part) => part.startsWith("jdk-") &&
    existsSync(path.join(bundledJdk, part, "bin", "java.exe")));
  if (name) env.JAVA_HOME = path.join(bundledJdk, name);
}

if (env.JAVA_HOME) env.Path = `${path.join(env.JAVA_HOME, "bin")}${path.delimiter}${env.Path || env.PATH || ""}`;

const localMaven = path.join(bundledJdk, "apache-maven-3.9.11", "bin", "mvn.cmd");
const maven = existsSync(localMaven) ? localMaven : "mvn.cmd";
const command = process.platform === "win32" ? "cmd.exe" : maven;
const args = process.platform === "win32" ? ["/d", "/s", "/c", `""${maven}" spring-boot:run"`] : ["spring-boot:run"];
const processHandle = spawn(command, args, {
  cwd: root,
  env,
  stdio: "inherit",
});

processHandle.on("error", (error) => {
  console.error("Unable to start Maven. Install JDK 17 and Maven 3.9+.", error.message);
  process.exitCode = 1;
});
processHandle.on("exit", (code) => { process.exitCode = code ?? 1; });