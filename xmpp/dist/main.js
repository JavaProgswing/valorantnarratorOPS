"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
const ConfigMITM_1 = require("./ConfigMITM");
const XmppMITM_1 = require("./XmppMITM");
const riotClientUtils_1 = require("./riotClientUtils");
const node_child_process_1 = require("node:child_process");
(async () => {
    const httpPort = 35479;
    const xmppPort = 35478;
    const host = '127.0.0.1';
    if (await (0, riotClientUtils_1.isRiotClientRunning)()) {
        console.log(JSON.stringify({
            type: 'error',
            code: 409,
            reason: 'Riot client is running, please close it before running this tool.'
        }) + '\n');
        process.exit(1);
    }
    const configMitm = new ConfigMITM_1.ConfigMITM(httpPort, host, xmppPort);
    await configMitm.start();
    const xmppMitm = new XmppMITM_1.XmppMITM(xmppPort, host, configMitm);
    await xmppMitm.start();
    const riotClientPath = await (0, riotClientUtils_1.getRiotClientPath)();
    (0, node_child_process_1.exec)(`"${riotClientPath}" --client-config-url="http://${host}:${httpPort}" --launch-product=valorant --launch-patchline=live`);
})();
