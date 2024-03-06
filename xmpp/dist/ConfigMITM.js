"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
exports.ConfigMITM = void 0;
const http = require("node:http");
class ConfigMITM {
    affinityMappings = [];
    _port;
    _host;
    _xmppPort;
    _server = null;
    _affinityMappingID = 0;
    constructor(port, host, xmppPort) {
        this._port = port;
        this._host = host;
        this._xmppPort = xmppPort;
    }
    async start() {
        this._server = http.createServer(async (req, res) => {
            const now = Date.now();
            const proxiedHeaders = new Headers(req.headers);
            proxiedHeaders.delete('host');
            const response = await fetch(`https://clientconfig.rpg.riotgames.com${req.url}`, {
                method: req.method,
                headers: proxiedHeaders
            });
            const text = await response.text();
            res.writeHead(response.status);
            if (req.url?.startsWith('/api/v1/config/player') && response.status === 200) {
                const data = JSON.parse(text);
                if (data.hasOwnProperty('chat.affinities')) {
                    for (const [region, ip] of Object.entries(data['chat.affinities'])) {
                        const existingMapping = this.affinityMappings.find(mapping => mapping.riotHost === ip);
                        if (existingMapping !== undefined) {
                            data['chat.affinities'][region] = existingMapping.localHost;
                        } else {
                            const newMapping = {
                                localHost: `127.0.0.${++this._affinityMappingID}`,
                                riotHost: ip,
                                riotPort: data['chat.port']
                            };
                            this.affinityMappings.push(newMapping);
                            data['chat.affinities'][region] = newMapping.localHost;
                        }
                    }
                    data['chat.port'] = this._xmppPort;
                    data['chat.host'] = this._host;
                    data['chat.allow_bad_cert.enabled'] = true;
                }
                res.write(JSON.stringify(data));
            } else {
                res.write(text);
            }
            res.end();
        });
        return new Promise((resolve, reject) => {
            this._server?.listen(this._port, this._host, () => {
                resolve();
            });
        });
    }
    async stop() {
        return new Promise((resolve, reject) => {
            this._server?.close(() => {
                resolve();
            });
        });
    }
}
exports.ConfigMITM = ConfigMITM;
