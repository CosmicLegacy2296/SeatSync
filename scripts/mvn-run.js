#!/usr/bin/env node
/**
 * Runs Maven using bundled ./maven_local when present, otherwise system `mvn`.
 * Usage: node scripts/mvn-run.js <maven-args...>
 */
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const projectRoot = path.join(__dirname, '..');
const isWin = process.platform === 'win32';
const mvnName = isWin ? 'mvn.cmd' : 'mvn';
const bundledMvn = path.join(projectRoot, 'maven_local', 'apache-maven-3.9.9', 'bin', mvnName);

function parseDotEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const content = fs.readFileSync(filePath, 'utf8');
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }

    const separatorIndex = line.indexOf('=');
    if (separatorIndex <= 0) {
      continue;
    }

    const key = line.slice(0, separatorIndex).trim();
    if (!key || process.env[key] !== undefined) {
      continue;
    }

    let value = line.slice(separatorIndex + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    process.env[key] = value;
  }
}

function normalizeDatabaseUrlForSpring() {
  const raw = process.env.DATABASE_URL;
  if (!raw) {
    return;
  }

  if (raw.startsWith('jdbc:')) {
    process.env.JDBC_DATABASE_URL = process.env.JDBC_DATABASE_URL || raw;
    return;
  }

  if (!(raw.startsWith('postgres://') || raw.startsWith('postgresql://'))) {
    return;
  }

  let parsed;
  try {
    parsed = new URL(raw);
  } catch {
    return;
  }

  const jdbcBase = `jdbc:postgresql://${parsed.host}${parsed.pathname}`;
  const params = new URLSearchParams(parsed.searchParams);
  const jdbcUrl = params.toString() ? `${jdbcBase}?${params.toString()}` : jdbcBase;

  process.env.JDBC_DATABASE_URL = process.env.JDBC_DATABASE_URL || jdbcUrl;
  process.env.JDBC_DATABASE_DRIVER = process.env.JDBC_DATABASE_DRIVER || 'org.postgresql.Driver';

  if (!process.env.JDBC_DATABASE_USERNAME && parsed.username) {
    process.env.JDBC_DATABASE_USERNAME = decodeURIComponent(parsed.username);
  }
  if (!process.env.JDBC_DATABASE_PASSWORD && parsed.password) {
    process.env.JDBC_DATABASE_PASSWORD = decodeURIComponent(parsed.password);
  }
}

function resolveMaven() {
  if (fs.existsSync(bundledMvn)) {
    return bundledMvn;
  }
  return mvnName;
}

function resolveJavaHome() {
  if (process.env.JAVA_HOME) {
    return process.env.JAVA_HOME;
  }

  function resolveWindowsFallbackJavaHome() {
    if (!isWin) {
      return null;
    }
    const winCandidates = [
      'C:\\Program Files\\AdoptOpenJDK\\jdk-17.0.0.20-hotspot',
      'C:\\Program Files\\Eclipse Adoptium\\jdk-17',
      'C:\\Program Files\\Java\\jdk-17',
    ];
    return winCandidates.find((candidate) => fs.existsSync(path.join(candidate, 'bin', 'java.exe'))) || null;
  }

  const cmd = isWin ? 'where' : 'which';
  const result = spawnSync(cmd, ['java'], { encoding: 'utf8', shell: false });
  if (result.status !== 0 || !result.stdout) {
    return resolveWindowsFallbackJavaHome();
  }

  const javaExec = result.stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line.length > 0);

  if (!javaExec) {
    return resolveWindowsFallbackJavaHome();
  }

  // java.exe is in <JAVA_HOME>/bin/java(.exe)
  return path.dirname(path.dirname(javaExec));
}

const mvn = resolveMaven();
const args = process.argv.slice(2);

parseDotEnv(path.join(projectRoot, '.env'));
normalizeDatabaseUrlForSpring();

const javaHome = resolveJavaHome();
if (javaHome) {
  process.env.JAVA_HOME = javaHome;
}

if (args.length === 0) {
  console.error('Usage: node scripts/mvn-run.js <maven-args...>');
  process.exit(1);
}

if (mvn === mvnName && !fs.existsSync(bundledMvn)) {
  console.log('Note: Using Maven from PATH (bundled maven_local not found).');
  console.log('Install Maven with: brew install maven\n');
}

let result;
if (isWin && mvn.toLowerCase().endsWith('.cmd')) {
  const commandLine = `"${mvn}" ${args.join(' ')}`;
  result = spawnSync('cmd.exe', ['/d', '/c', commandLine], {
    cwd: projectRoot,
    stdio: 'inherit',
    shell: false,
    windowsVerbatimArguments: true,
  });
} else {
  result = spawnSync(mvn, args, {
    cwd: projectRoot,
    stdio: 'inherit',
    shell: false,
  });
}

if (result.error) {
  console.error('\nFailed to run Maven:', result.error.message);
  if (mvn === mvnName) {
    console.error('Install Java 17+ and Maven, then try again.');
    console.error('  macOS: brew install maven');
  }
  process.exit(1);
}

process.exit(result.status === null ? 1 : result.status);
