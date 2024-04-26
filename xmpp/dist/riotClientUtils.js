"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
exports.getRiotClientPath = exports.isRiotClientRunning = void 0;
const node_child_process_1 = require("node:child_process");
async function isRiotClientRunning() {
    return new Promise(resolve => {
        (0, node_child_process_1.exec)('tasklist /fi "imagename eq RiotClientServices.exe"', (error, stdout, stderr) => {
            resolve(stdout.includes('RiotClientServices.exe'));
        });
    });
}
exports.isRiotClientRunning = isRiotClientRunning;
const installStringRegex = new RegExp('    UninstallString    REG_SZ    "(.+?)" --uninstall-product=valorant --uninstall-patchline=live');
async function getRiotClientPath() {
    return new Promise((resolve, reject) => {
        (0, node_child_process_1.exec)('reg query "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Riot Game valorant.live" /v UninstallString', (error, stdout, stderr) => {
            const lines = stdout.split('\r\n');
            for (const line of lines) {
                if (line.startsWith('ERROR')) {
                    return reject(new Error(line));
                } else {
                    const match = installStringRegex.exec(line);
                    if (match !== null) {
                        return resolve(match[1]);
                    }
                }
            }
        });
    });
}
exports.getRiotClientPath = getRiotClientPath;
