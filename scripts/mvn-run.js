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

function resolveMaven() {
  if (fs.existsSync(bundledMvn)) {
    return bundledMvn;
  }
  return mvnName;
}

const mvn = resolveMaven();
const args = process.argv.slice(2);

if (args.length === 0) {
  console.error('Usage: node scripts/mvn-run.js <maven-args...>');
  process.exit(1);
}

if (mvn === mvnName && !fs.existsSync(bundledMvn)) {
  console.log('Note: Using Maven from PATH (bundled maven_local not found).');
  console.log('Install Maven with: brew install maven\n');
}

const result = spawnSync(mvn, args, {
  cwd: projectRoot,
  stdio: 'inherit',
  shell: isWin,
});

if (result.error) {
  console.error('\nFailed to run Maven:', result.error.message);
  if (mvn === mvnName) {
    console.error('Install Java 17+ and Maven, then try again.');
    console.error('  macOS: brew install maven');
  }
  process.exit(1);
}

process.exit(result.status === null ? 1 : result.status);
