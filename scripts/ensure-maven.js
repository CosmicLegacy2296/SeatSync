#!/usr/bin/env node
/**
 * Ensures Maven is available: uses system mvn, or downloads 3.9.9 into ./maven_local.
 */
const { execSync, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const projectRoot = path.join(__dirname, '..');
const isWin = process.platform === 'win32';
const mvnName = isWin ? 'mvn.cmd' : 'mvn';
const bundledMvn = path.join(projectRoot, 'maven_local', 'apache-maven-3.9.9', 'bin', mvnName);
const MAVEN_VERSION = '3.9.9';
const MAVEN_URL = `https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz`;

function hasSystemMaven() {
  const check = spawnSync(isWin ? 'mvn.cmd' : 'mvn', ['-version'], { stdio: 'ignore', shell: isWin });
  return check.status === 0;
}

function hasJava() {
  const check = spawnSync('java', ['-version'], { stdio: 'ignore' });
  return check.status === 0;
}

if (fs.existsSync(bundledMvn)) {
  process.exit(0);
}

if (hasSystemMaven()) {
  process.exit(0);
}

if (!hasJava()) {
  console.error('Java 17+ is required. Install from https://adoptium.net/');
  process.exit(1);
}

console.log(`Maven not found. Downloading Apache Maven ${MAVEN_VERSION} to maven_local/ (one-time setup)...\n`);

const mavenLocalDir = path.join(projectRoot, 'maven_local');
fs.mkdirSync(mavenLocalDir, { recursive: true });

try {
  if (process.platform === 'win32') {
    const zipUrl = `https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip`;
    const zipPath = path.join(mavenLocalDir, `apache-maven-${MAVEN_VERSION}-bin.zip`);
    execSync(`curl -fsSL "${zipUrl}" -o "${zipPath}"`, { stdio: 'inherit', cwd: projectRoot });
    execSync(`tar -xf "${zipPath}" 2>nul || powershell -Command "Expand-Archive -Force '${zipPath}' '${mavenLocalDir}'"`, {
      stdio: 'inherit',
      cwd: projectRoot,
      shell: true,
    });
  } else {
    execSync(`curl -fsSL "${MAVEN_URL}" | tar -xz -C "${mavenLocalDir}"`, {
      stdio: 'inherit',
      cwd: projectRoot,
      shell: true,
    });
  }
} catch (err) {
  console.error('\nCould not download Maven automatically.');
  console.error('Install manually: brew install maven');
  console.error('Or place Maven at: maven_local/apache-maven-3.9.9/');
  process.exit(1);
}

if (!fs.existsSync(bundledMvn)) {
  console.error('\nMaven download finished but mvn binary was not found at:', bundledMvn);
  process.exit(1);
}

console.log('\nMaven ready.\n');
